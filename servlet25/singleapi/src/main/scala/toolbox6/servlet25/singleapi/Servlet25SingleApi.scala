package toolbox6.servlet25.singleapi

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Created by pappmar on 30/08/2016.
  */
trait Servlet25SingleApi {

  def set(handler: Option[Servlet25SingleHandler]) : Unit

  def get() : Option[Servlet25SingleHandler]

}

trait Servlet25SingleHandler {
  def handle(req: HttpServletRequest, response: HttpServletResponse) : Unit
}

