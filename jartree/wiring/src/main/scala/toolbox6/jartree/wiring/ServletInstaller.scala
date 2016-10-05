package toolbox6.jartree.wiring

import toolbox6.jartree.api._
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.{ClassRequestImpl, RunTools}

/**
  * Created by pappmar on 04/10/2016.
  */
abstract class ServletInstaller
  extends ClosableJarPlugger[Processor]
    with Processor
{


  override def run(ctx: JarContext[JarTreeServletContext], self: ClassRequest[ServletInstaller]): Unit = {
    ctx
      .extension()
        .processor()
        .plug(

        )
      .setProcessor(
        this
      )
  }

  override def run(data: Array[Byte], ctx: JarContext[JarTreeServletContext], self: ClassLoaderKey): Array[Byte] = {
    RunTools.runBytesAll(
      { () =>
        ctx.setStartup(
          ClassRequestImpl[JarRunnable[JarTreeServletContext]](
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
