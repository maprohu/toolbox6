package toolbox6.pickling

import java.io.File
import java.nio.ByteBuffer

import toolbox6.common.ByteBufferTools

/**
  * Created by pappmar on 20/10/2016.
  */
object PicklingTools extends boopickle.Base with
  boopickle.BasicImplicitPicklers with
  boopickle.TransformPicklers with
  boopickle.TuplePicklers with
  boopickle.MaterializePicklerFallback {


  def unpickle[T](data: Array[Byte])(implicit u: Pickler[T]) : T = {
    Unpickle[T].fromBytes(ByteBuffer.wrap(data))
  }

  def pickle[T](value: T)(implicit p: Pickler[T]) : Array[Byte] = {
    toByteArray(Pickle(value).toByteBuffer)
  }

  def toByteArray(bb: ByteBuffer) : Array[Byte] = {
    val ba = Array.ofDim[Byte](bb.remaining())
    bb.get(ba)
    ba
  }

  def toFile[T](value: T, file: File)(implicit p: Pickler[T]) : Unit = {
    ByteBufferTools
      .writeFile(
        Pickle(value)
          .toByteBuffer,
        file
      )
  }

  def fromFile[T](file: File)(implicit p: Pickler[T]) : T = {
    Unpickle[T]
      .fromBytes(
        ByteBufferTools
          .readFile(
            file
          )
      )
  }

}
