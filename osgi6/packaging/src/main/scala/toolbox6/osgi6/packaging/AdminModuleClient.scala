package toolbox6.osgi6.packaging

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by pappmar on 28/09/2016.
  */
object AdminModuleClient {

  val roots =
    Seq
      .apply(
        emsamg.modules.RunEmsaManaged.Roots
      )
      .foldLeft(Seq.empty[PlacedRoot])(_ ++ _)
      .map(r => r.rootContainer -> r.rootDir)
      .toMap

  def run(
    module : NamedModule,
    target: Uri
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) = {

    val projectDir =
      Module.projectDir(
        module,
        roots
      )


    AdminClient.build(projectDir)

    MavenTools
      .bundle(
        module
      )({ jar =>
        Await.ready(
          AdminClient.runJar(
            jar,
            target
          ),
          Duration.Inf
        )
      })

  }

  val BundleFileName = "bundle.jar"

  sealed trait DeployAction
  object Deploy extends DeployAction
  object Redeploy extends DeployAction

  def symbolicName(action: DeployAction): String = {
    val last = action match {
      case Deploy => "deploy"
      case Redeploy => "redeploy"
    }

    symbolicName(last)
  }

  def symbolicName(action: String): String = {
    (Lib6Modules.AdminTools.path :+ action).mkString(".")
  }






  def deployBundle(
    module : DeployableModule,
    target: Uri,
    action: DeployAction = Deploy
  )(implicit
    bactorSystem: ActorSystem,
    materializer: Materializer
  ) = {
    deploy(
      DeployableModuleImpl(
        groupId = module.groupId,
        artifactId = MavenTools.bundleArtifactId(module.artifactId),
        version = module.version
      ),
      target,
      action
    )
  }

  trait HasSymbolicName {
    def symbolicName: String
  }


  object HasSymbolicName {
    implicit def fromString(str: String) = new HasSymbolicName {
      override def symbolicName: String = str
    }
    implicit def fromModule(module: NamedModule) : HasSymbolicName = module.pkg
  }

  def cleanup(
    retain: Seq[HasSymbolicName],
    target: Uri
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) = {
    MavenTools
      .bundle(
        module = Lib6Modules.AdminTools,
        symbolicName = Some(symbolicName("cleanup")),
        embeddeds = Seq(
          EmbeddedArtifact(
            filename = "cleanuplist.txt",
            module =
              retain
                .map(_.symbolicName)
                .mkString("\n")
          )
        )
      )({ jar =>
        Await.ready(
          AdminClient.runJar(
            jar,
            target
          ),
          Duration.Inf
        )
//        Future.successful()
      })
  }

  def deploy(
    module : DeployableModule,
    target: Uri,
    action: DeployAction = Deploy
  )(implicit
    actorSystem: ActorSystem,
    materializer: Materializer
  ) = {

    MavenTools
      .bundle(
        module = Lib6Modules.AdminTools,
        symbolicName = Some(symbolicName(action)),
        embeddeds = Seq(
          EmbeddedArtifact(
            filename = BundleFileName,
            module = module
          )
        )
      )({ jar =>
        Await.ready(
          AdminClient.runJar(
            jar,
            target
          ),
          Duration.Inf
        )
      })

  }

}
