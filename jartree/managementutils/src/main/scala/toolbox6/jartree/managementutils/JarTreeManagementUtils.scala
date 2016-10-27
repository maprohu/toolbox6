package toolbox6.jartree.managementutils

import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.wiring.PlugRequestImpl

/**
  * Created by martonpapp on 02/10/16.
  */
object JarTreeManagementUtils {

  val MonitoringName = "monitoring"

  def bindingNamePath(
    app: String
  ) = {
    Seq(app)
  }

  def bindingName(
    app: String
  ) = {
    (bindingNamePath(app) :+ MonitoringName).mkString(".")
  }

}


