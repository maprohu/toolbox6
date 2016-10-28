package toolbox6.common

/**
  * Created by pappmar on 30/08/2016.
  */
class SingleRegistry[Value] {

  @volatile var value = Option.empty[Value]

  def set(value: Option[Value]) : Unit = {
    this.value = value
  }

  def get() : Option[Value] = {
    value
  }

}
