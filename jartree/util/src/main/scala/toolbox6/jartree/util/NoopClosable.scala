package toolbox6.jartree.util


/**
  * Created by Student on 06/10/2016.
  */
trait NoopClosable extends Closable {
  override def close(): Unit = ()
}
