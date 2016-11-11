package akka.actor
import akka.actor.ActorSystem.Settings
import akka.dispatch.{Dispatchers, Mailboxes}
import akka.event.{EventStream, LoggingAdapter}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.Duration

/**
  * Created by pappmar on 11/11/2016.
  */
class FakeActorSystem extends ActorSystem {
  override def name: String = ???

  override def settings: Settings = new Settings(
    classOf[ActorSystem].getClassLoader,
    ConfigFactory.load(
      classOf[ActorSystem].getClassLoader
    ),
    "fake"
  )

  override def logConfiguration(): Unit = ???

  override def /(name: String): ActorPath = ???

  override def /(name: Iterable[String]): ActorPath = ???

  override def eventStream: EventStream = ???

  override def log: LoggingAdapter = ???

  override def deadLetters: ActorRef = ???

  override def scheduler: Scheduler = ???

  override def dispatchers: Dispatchers = ???

  override implicit def dispatcher: ExecutionContextExecutor = ???

  override def mailboxes: Mailboxes = ???

  override def registerOnTermination[T](code: => T): Unit = ???

  override def registerOnTermination(code: Runnable): Unit = ???

  override def awaitTermination(timeout: Duration): Unit = ???

  override def awaitTermination(): Unit = ???

  override def shutdown(): Unit = ???

  override def isTerminated: Boolean = ???

  override def registerExtension[T <: Extension](ext: ExtensionId[T]): T = ???

  override def extension[T <: Extension](ext: ExtensionId[T]): T = ???

  override def hasExtension(ext: ExtensionId[_ <: Extension]): Boolean = ???

  override protected def systemImpl: ActorSystemImpl = ???

  override protected def provider: ActorRefProvider = ???

  override protected def guardian: InternalActorRef = ???

  override protected def lookupRoot: InternalActorRef = ???

  override def actorOf(props: Props): ActorRef = ???

  override def actorOf(props: Props, name: String): ActorRef = ???

  override def stop(actor: ActorRef): Unit = ???
}

object FakeActorSystem {
  def apply(): FakeActorSystem = new FakeActorSystem()
}
