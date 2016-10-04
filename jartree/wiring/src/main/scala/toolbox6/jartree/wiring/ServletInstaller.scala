package toolbox6.jartree.wiring

import toolbox6.jartree.api._
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.{RunRequestImpl, RunTools}

/**
  * Created by pappmar on 04/10/2016.
  */
abstract class ServletInstaller
  extends JarRunnable[JarTreeServletContext]
    with JarRunnableByteArray[JarTreeServletContext]
    with Processor
{

  override def run(ctx: JarContext[JarTreeServletContext], self: ClassLoaderKey): Unit = {
    ctx
      .extension()
      .setProcessor(
        this
      )
  }

  override def run(data: Array[Byte], ctx: JarContext[JarTreeServletContext], self: ClassLoaderKey): Array[Byte] = {
    RunTools.runBytesAll(
      { () =>
        ctx.setStartup(
          RunRequestImpl(
            self,
            getClass.getName
          )
        )
        s"Startup set to: ${getClass.getName}"
      },
      { () =>
        run(ctx, self)
        s"Started: ${getClass.getName}"
      }
    )
  }

  override def close(): Unit = ()
}
