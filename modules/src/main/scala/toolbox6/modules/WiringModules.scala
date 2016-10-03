package toolbox6.modules

import maven.modules.builder.{ScalaModule, SubModuleContainer}

/**
  * Created by pappmar on 30/08/2016.
  */
object WiringModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "wiring")


  object Single extends ScalaModule(
    "single",
    "1.0.0-SNAPSHOT",
    mvn.`io.monix:monix-execution_2.11:jar:2.0.2`,
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`

  )


}
