package toolbox6.jartree.packaging

import java.io.File

import weblogic.deploy.api.tools.deployer.Deployer

/**
  * Created by martonpapp on 02/10/16.
  */
object WeblogicDeployer {

  case class ConnectionConfig(
    host: String = "localhost",
    port: Int = 7002,
    user: String = "weblogic",
    password: String = "weblogic1"
  )

  case class DeployConfig(
    war: File,
    targets: Option[Seq[String]] = None
  )

  def run(
    connectionConfig: ConnectionConfig,
    deployConfig: DeployConfig
  ) = {
//    new Deployer(
//      Array("-help")
//    ).run()
//    new Deployer(
//      Array("-examples")
//    ).run()
//    new Deployer(
//      Array("-advanced")
//    ).run()

    new Deployer(
      Array(
        "-adminurl", s"t3://${connectionConfig.host}:${connectionConfig.port}",
        "-username", connectionConfig.user,
        "-password", connectionConfig.password,
        "-deploy", deployConfig.war.getAbsolutePath,
        "-upload"
      ) ++
        deployConfig
          .targets
          .toSeq
          .flatMap(t => Seq("-targets", t.mkString(",")))
    ).run()


//    import connectionConfig._
//    val serviceURL = new JMXServiceURL(
//      "t3",
//      host,
//      port,
//      "/jndi/weblogic.management.mbeanservers.domainruntime"
//    )
//
//    val h = new util.Hashtable[String, String]()
//    h.put(Context.SECURITY_PRINCIPAL, user)
//    h.put(Context.SECURITY_CREDENTIALS, password)
//    h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote")
//    val connector = JMXConnectorFactory.connect(serviceURL, h)
//    val connection = connector.getMBeanServerConnection
//
//    val svcBean =
//      weblogic.management.jmx.MBeanServerInvocationHandler.newProxyInstance(
//        connection,
//        new ObjectName(DomainRuntimeServiceMBean.OBJECT_NAME)
//      ).asInstanceOf[DomainRuntimeServiceMBean]
//
//    val deploymentManager = svcBean.getDomainRuntime.getDeploymentManager
//
//    connection.addNotificationListener(
//      new ObjectName("com.bea:Name=DeploymentManager,Type=DeploymentManager"),
//      new NotificationListener {
//        override def handleNotification(notification: Notification, handback: scala.Any): Unit = {
//          println(s"notification: ${notification.getType} -> ${notification.getUserData}")
//        }
//      },
//      null,
//      null
//    )
//
//    val progress = deploymentManager.deploy(
//      deployConfig.name,
//      deployConfig.war.getAbsolutePath,
//      null
//    )
//
//    println(progress.getState)
//    progress.getRootExceptions.foreach(_.printStackTrace())
  }


}
