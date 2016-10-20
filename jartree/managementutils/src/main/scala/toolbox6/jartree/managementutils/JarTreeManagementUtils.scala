package toolbox6.jartree.managementutils

import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.wiring.PlugRequestImpl

/**
  * Created by martonpapp on 02/10/16.
  */
object JarTreeManagementUtils {

  def bindingName(
    app: String
  ) = {
    s"${app}.monitoring"
  }

}

case class QueryResult(
  request: Option[PlugRequestImpl[Processor, JarTreeServletContext]],
  webappVersion: String
)
