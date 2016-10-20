package toolbox6.pickling

import java.nio.ByteBuffer

import boopickle._
/**
  * Created by pappmar on 20/10/2016.
  */
object PicklingTools extends Base with
  BasicImplicitPicklers with
  TransformPicklers with
  TuplePicklers with
  MaterializePicklerFallback {


  def unpickle[T](data: Array[Byte])(implicit u: Pickler[T]) = {
    Unpickle[T].fromBytes(ByteBuffer.wrap(data))
  }

  def pickle[T](value: T)(implicit p: Pickler[T]) = {
    toByteArray(Pickle(value).toByteBuffer)
  }

  def toByteArray(bb: ByteBuffer) : Array[Byte] = {
    val ba = Array.ofDim[Byte](bb.remaining())
    bb.get(ba)
    ba
  }


}
