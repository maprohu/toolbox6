package toolbox6.jartree.packaging

import toolbox6.jartree.impl.JarTreeInitializer

/**
  * Created by pappmar on 21/11/2016.
  */
object JarTreeBootstrapPackaging {

}


case class JatTreeBootstrapMeta[T, C] (
  name: String,
  dataPath: String,
  logPath: String,
  storagePath: Option[String],
  version : Option[String],
  initializer: JarTreeInitializer[T, C]
)

