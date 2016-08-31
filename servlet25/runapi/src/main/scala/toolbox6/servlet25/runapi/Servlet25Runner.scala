package toolbox6.servlet25.runapi

import javax.servlet.http.HttpServlet

import jartree.{JarTreeRunner, RunRequest}
import toolbox6.servlet25.singleapi.Servlet25SingleHandler

/**
  * Created by pappmar on 30/08/2016.
  */
trait Servlet25Runner {

  def run(context: Servlet25Context) : Unit

}

trait Servlet25Context {

  def servlet() : HttpServlet

  def jarTree() : JarTreeRunner

  def setStartup(runRequest: RunRequest) : Unit

  def setHandler(handler: Option[Servlet25SingleHandler]) : Unit

}
