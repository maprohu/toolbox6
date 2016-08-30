package toolbox6.servlet25.samplerunner

import toolbox6.servlet25.runapi.{Servlet25Context, Servlet25Runner}

/**
  * Created by pappmar on 30/08/2016.
  */
class SampleRunnerImpl extends Servlet25Runner {
  override def run(context: Servlet25Context): Unit = {
    println("booooooooo")
  }
}
