package toolbox6.akka.http

import akka.http.scaladsl.model.Uri.Query

/**
  * Created by pappmar on 11/10/2016.
  */
object RestTools {


}

object RestRequest {
  trait AssignmentLike {
    def name: String
    def valueString: String
  }
  trait AssignmentsLike {
    def seq : Seq[AssignmentLike]
    def query : Query = Query(
      seq.map(a => a.name -> a.valueString).toMap
    )
  }

  case class Assignments(
    seq: Seq[AssignmentLike]
  ) extends AssignmentsLike {
    def &&(o: AssignmentLike) = copy(seq = seq :+ o)
  }

  case class Assignment[T](
    param: Param[T],
    value: T
  ) extends AssignmentLike with AssignmentsLike {
    def seq = Seq(this)
    def &&(o: AssignmentLike) = Assignments(Seq(this, o))
    def name = param.name
    def valueString = param.asString(value)
  }

  trait Param[T] {
    def name: String
    def asString(value: T) : String
    def <<(value: T) = Assignment(this, value)
  }
}
class RestRequest {
  import RestRequest._

  implicit class StringParam(val name: String) extends Param[String] {
    override def asString(value: String): String = value
  }
  implicit class IntParam(val name: String) extends Param[Int] {
    override def asString(value: Int): String = value.toString
  }
  implicit class FloatParam(val name: String) extends Param[Float] {
    override def asString(value: Float): String = value.toString
  }

  private var _params = Seq.empty[Param[_]]

  def params() = _params

  protected def param[T](name: String)(implicit fn: String => Param[T]) : Param[T] = {
    val p = fn(name)
    _params :+= p
    p
  }

}
