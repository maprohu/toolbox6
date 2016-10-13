package toolbox6.jartree.util

import toolbox6.jartree.api.Closable

/**
  * Created by Student on 06/10/2016.
  */
trait NoopClosable extends Closable {
  override def close(): Unit = ()
}
