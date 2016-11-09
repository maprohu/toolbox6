package toolbox6.jms

import javax.jms.ConnectionFactory

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.camel.CamelExtension
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.Atomic
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.impl.JndiRegistry
import org.springframework.jms.support.destination.JndiDestinationResolver
import toolbox6.logging.LogTools

import scala.concurrent.{Future, Promise}

/**
  * Created by pappmar on 09/11/2016.
  */
object JmsTools extends LazyLogging with LogTools {

  val ids = Atomic(0)
  def nextId = ids.transformAndExtract({ id =>
    (s"jms${id}", id+1)
  })

  case class Config(
    jndiEnvironment: java.util.Properties,
    destination: String,
    connectionFactoryName: String = "weblogic.jms.ConnectionFactory",
    componentId: String = nextId
  )

  def setup(
    config: Config
  )(implicit
    actorSystem: ActorSystem
  ) : String = {
    import config._
    val camel = CamelExtension(actorSystem)

    val jndiRegistry =
      new JndiRegistry(jndiEnvironment)

    val resolver = new JndiDestinationResolver
    resolver.setJndiEnvironment(
      jndiEnvironment
    )

    val connectionFactory =
      jndiRegistry
        .lookup(
          connectionFactoryName
        )
        .asInstanceOf[ConnectionFactory]

    jndiRegistry.close()

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

    s"${componentId}:noname?destinationName=${destination}"
  }

  def teardown(
    componentId: String
  )(implicit
    actorSystem: ActorSystem
  ) = {
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
                  onCompleteMessage = PoisonPill
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

  def source(
    config: Config,
    bufferSize: Int = 1024,
    strategy: OverflowStrategy = OverflowStrategy.dropHead
  )(implicit
    actorSystem: ActorSystem
  ) : Source[Any, _] = {
    Source
      .actorRef(bufferSize, strategy)
      .mapMaterializedValue({ ref =>
        val uri = setup(config)

        actorSystem.actorOf(
          Props(
            classOf[CamelJmsReceiverActor],
            CamelJmsReceiverActor.Config(
              uri = uri,
              target = ref
            )
          )
        )


      })
  }


}
