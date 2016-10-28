package toolbox6.javaapi

trait AsyncCallback[-O] {
  def success(value: O)
  def failure(throwable: Throwable)
}
trait AsyncValue[+O] {
  def onComplete(cb: AsyncCallback[O]) : Unit
}
trait AsyncFunction[-I, +O] {
  def apply(input: I) : AsyncValue[O]
}
