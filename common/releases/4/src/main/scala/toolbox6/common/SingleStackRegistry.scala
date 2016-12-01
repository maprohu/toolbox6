package toolbox6.common

/**
  * Created by pappmar on 30/08/2016.
  */
class SingleStackRegistry[Value, Registration](
  unreg: (() => Unit) => Registration
) {

  @volatile var values = Seq.empty[Value]

  def remove(value: Value) : Unit = synchronized {
    values = values diff Seq(value)
  }

  def add(value: Value) : Unit = synchronized {
    values = value +: values
  }

  def register(value: Value) : Registration = {
    add(value)

    unreg({ () =>
      remove(value)
    })
  }

  def current(fallback: Value) : Value = {
    values.headOption.getOrElse(fallback)
  }


}
