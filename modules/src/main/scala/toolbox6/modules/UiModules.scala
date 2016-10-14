package toolbox6.modules

import maven.modules.builder.{ScalaModule, SubModuleContainer}

/**
  * Created by pappmar on 30/08/2016.
  */
object UiModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "ui")

  object Ast extends ScalaModule(
    "ast",
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`
  )

  object Swing extends ScalaModule(
    "swing",
    Ast
  )

  object Android extends ScalaModule(
    "android",
    Ast,
    mvn.`com.google.android:android:jar:4.1.1.4`
  )


}
