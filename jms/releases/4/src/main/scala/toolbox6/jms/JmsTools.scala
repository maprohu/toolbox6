package toolbox6.jms

import javax.jms.{Connection, ConnectionFactory}
import javax.naming.InitialContext

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, PoisonPill, Props}
import akka.camel.CamelExtension
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.atomic.Atomic
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.impl.JndiRegistry
import toolbox6.logging.LogTools

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import akka.pattern._
import akka.util.Timeout

/**
  * Created by pappmar on 09/11/2016.
  */
object JmsTools extends LazyLogging with LogTools {

  val ids = Atomic(0)
  def nextId(prefix: String) = ids.transformAndExtract({ id =>
    (s"${prefix}${id}", id+1)
  })

  case class Config(
    jndiEnvironment: java.util.Properties,
    destination: String,
    connectionFactoryName: String = "weblogic.jms.ConnectionFactory",
    componentId: String = nextId("jms")
  )

  def safeContex[T](what: => T) : T = {
    val ccl = Thread.currentThread().getContextClassLoader

    try {
      Thread.currentThread().setContextClassLoader(
        classOf[ConnectionFactory].getClassLoader
      )

      what
    } finally {
      Thread.currentThread().setContextClassLoader(
        ccl
      )
    }
  }

  def setup(
    config: Config
  )(implicit
    actorSystem: ActorSystem
  ) : String = {
    import config._
    val camel = CamelExtension(actorSystem)

    logger.info("connecting to: {}", jndiEnvironment)

    val resolver = new SafeDestinationResolver
    resolver.setJndiEnvironment(
      jndiEnvironment
    )

    val connectionFactory = safeContex {
      val jndiRegistry =
        new JndiRegistry(
          new InitialContext(
            jndiEnvironment
          )
        )


      try {
        val cf = jndiRegistry
          .lookup(
            connectionFactoryName
          )
          .asInstanceOf[ConnectionFactory]

        new ConnectionFactory {
          override def createConnection(): Connection =
            safeContex(cf.createConnection())
          override def createConnection(userName: String, password: String): Connection =
            safeContex(cf.createConnection(userName, password))
        }
      } finally {
        jndiRegistry.close()
      }
    }

    val jmsComponent = new JmsComponent(camel.context)
    jmsComponent
      .setConnectionFactory(
        connectionFactory
      )
    jmsComponent
      .setDestinationResolver(
        resolver
      )

    camel
      .context
      .addComponent(componentId, jmsComponent)

    s"${componentId}:${destination}?destinationName=${destination}"
  }

  def teardown(
    componentId: String
  )(implicit
    actorSystem: ActorSystem
  ) = {
    logger.info(s"removing component: ${componentId}")
    val camel = CamelExtension(actorSystem)

    camel
      .context
      .removeComponent(componentId)
  }


  def sink(
    config: Config
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) : Sink[Any, Future[Unit]] = {
    import config._

    Flow[Any]
      .prefixAndTail(0)
      .flatMapConcat({
        case (_, source) =>
          import actorSystem.dispatcher

          val uri = setup(
            config
          )

          val promise = Promise[Unit]()

          val ref = actorSystem.actorOf(
            Props(
              classOf[CamelJmsSenderAckActor],
              CamelJmsSenderAckActor.Config(
                uri = uri,
                promise = promise
              )
            )
          )

          source
            .runWith(
              Sink
                .actorRefWithAck(
                  ref = ref,
                  onInitMessage = CamelJmsSenderAckActor.Pull,
                  ackMessage = CamelJmsSenderAckActor.Ack,
                  onCompleteMessage = CamelJmsSenderAckActor.Complete
                )
            )

          Source.fromFuture(
            promise
              .future
              .andThen({
                case _ =>
                  quietly {
                    teardown(componentId)
                  }
              })
          )
      })
      .toMat(Sink.ignore)(Keep.right)
  }

  case class ConnectionStatus(
    activated: Future[Unit],
    stopped: Future[Unit]
  ) {
    def merge(
      other: ConnectionStatus
    )(implicit
      executionContext: ExecutionContext
    ) = {
      ConnectionStatus(
        stopped =
          Future
            .sequence(Seq(stopped, other.stopped))
            .map(_ => ()),
        activated =
          Future
            .sequence(Seq(activated, other.activated))
            .map(_ => ())
      )
    }
  }

  def source(
    config: Config,
    bufferSize: Int = 1024 * 4,
    strategy: OverflowStrategy = OverflowStrategy.dropHead
  )(implicit
    actorSystem: ActorSystem
  ) : Source[Any, ConnectionStatus] = {
    import config._
    implicit val timeout : Timeout = 15.seconds


    Source
      .actorRef(bufferSize, strategy)
      .mapMaterializedValue({ ref =>
        logger.info(s"starting source: ${config.destination}")

        val uri = setup(config)

        val promise = Promise[Unit]()

        val camelRef = actorSystem.actorOf(
          Props(
            classOf[CamelJmsReceiverActor],
            CamelJmsReceiverActor.Config(
              uri = uri,
              target = ref,
              promise = promise
            )
          ),
          nextId("jmsToolsSource")
        )

        logger.info(s"started camel actor: ${camelRef}")

        import actorSystem.dispatcher
        val stopped = promise
          .future
          .andThen({
            case _ =>
              quietly {
                teardown(componentId)
              }
          })

        ConnectionStatus(
          activated =
            CamelExtension(actorSystem)
              .activationFutureFor(camelRef)
              .map(_ => ()),
          stopped = stopped
        )
      })
  }

  def unbounded(
    config: Config
  )(implicit
    actorSystem: ActorSystem
  ) : (Any => Future[Unit], Cancelable) = {
    import config._
    import actorSystem.dispatcher
    logger.info(s"connecting to: ${destination} using factory: ${connectionFactoryName}")
    implicit val timeout : Timeout = 15.seconds
    val uri = setup(config)

    val ref = actorSystem.actorOf(
      Props(
        classOf[CamelJmsSenderActor],
        CamelJmsSenderActor.Config(
          uri = uri
        )
      ),
      nextId("jmsUnboundedSender")
    )

    val out = { msg:Any =>
      ref
        .ask(msg)
        .map(_ => ())
    }

    val cancel = Cancelable({ () =>
      logger.info(s"stopping sender: ${destination}")
      Await.result(
        gracefulStop(
          ref,
          15.seconds
        ).andThen({
          case _ =>
            teardown(componentId)
        }),
        Duration.Inf
      )
    })


    (out, cancel)
  }

}
