package toolbox6.servlet25.webapp

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import toolbox6.servlet25.singleapi.{Servlet25SingleHandler}

/**
  * Created by pappmar on 30/08/2016.
  */
class Servlet25WebappServlet extends HttpServlet with LazyLogging {

  val singleApi = new Servlet25SingleApiImpl

  val Fallback = new Servlet25SingleHandler {
    override def handle(req: HttpServletRequest, response: HttpServletResponse): Unit = ()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    try {
      singleApi.get().getOrElse(Fallback).handle(req, resp)
    } catch {
      case ex : Throwable =>
        logger.error("error serving request" , ex)
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }
  }

}
