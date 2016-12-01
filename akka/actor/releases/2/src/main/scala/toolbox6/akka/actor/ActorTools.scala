package toolbox6.akka.actor

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
  * Created by pappmar on 10/11/2016.
  */
object ActorTools {

  def createActorSystem(self: Any) = ActorSystem(
    s"${self.getClass.getSimpleName}ActorSystem",
    ConfigFactory.parseString(
      """
        |akka {
        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
        |  loglevel = "DEBUG"
        |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
        |  jvm-exit-on-fatal-error = false
        |  actor {
        |    default-dispatcher {
        |      executor = "thread-pool-executor"
        |    }
        |  }
        |}
      """.stripMargin
    ).withFallback(ConfigFactory.load()),
    self.getClass.getClassLoader
  )

}
