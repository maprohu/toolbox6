package toolbox6.tools

import java.io.Closeable


trait TF[R] {
  def extract[V](fn: R => V) : V

  def foreach(fn: R => Unit) : Unit = {
    extract(fn)
  }
  def flatMap[R2](fn: R => TF[R2]) = new NestedTF[R2, R](this, fn)
  def map[R2](fn: R => R2) : TF[R2] = new MappedTF[R2, R](this, fn)
}

class MappedTF[R2, R1](
  base: TF[R1],
  mapping: R1 => R2
) extends TF[R2] {
  override def extract[V](fn: (R2) => V): V = {
    base
      .extract({ r1 =>
        fn(mapping(r1))
      })
  }
}

class BaseTF[R](
  open: () => R,
  close: R => Unit
) extends TF[R] {

  override def extract[V](fn: R => V): V = {
    val r = open()
    try {
      fn(r)
    } finally {
      close(r)
    }
  }
}

class NestedTF[R2, R1](
  outer: TF[R1],
  mapping: R1 => TF[R2]
) extends TF[R2] {
  override def extract[V](fn: (R2) => V): V = {
    outer
      .extract({ r1 =>
        mapping(r1).extract(fn)
      })
  }
}

object TF {
  def apply[R](
    open: () => R,
    close: R => Unit
  ): BaseTF[R] = new BaseTF(open, close)
  def from[R](
    open: => R
  )(
    close: R => Unit
  ): BaseTF[R] = new BaseTF(() => open, close)

  def apply(
    c: => Closeable
  ) = from(c)(_.close())
}

