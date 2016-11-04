package toolbox6
import collection.immutable._

/**
  * Created by pappmar on 21/10/2016.
  */

package object environment {

  case class Host(
    name: String
  )

  trait Qualifier
  case class Qualification[Q <: Qualifier](
    qualifiers: Q*
  ) {
    require(qualifiers.distinct == qualifiers)

    val set : Set[Q] = qualifiers.to[Set]
  }
  class Dictionary[Q <: Qualifier, V](
    items: (Qualification[Q], V)*
  ) {
    def this(qs: Seq[(Qualification[Q], V)]) = this(qs:_*)

    private val map : Map[Set[Q], V] =
      items
        .map({ case (q, v) => (q.set, v)})(collection.breakOut)

    def get(
      qualifiers: Q*
    ) : Option[V] = {
      val qset = Qualification(qualifiers:_*).set
      map
        .get(qset)
        .orElse {
          val subSets =
            map
              .filterKeys(k => k subsetOf qset)

          if (subSets.isEmpty) {
            None
          } else {
            Some {
              val (rset, rvalue) =
                subSets
                  .maxBy({ case (k, _) => k.size })

              subSets
                .tail
                .foreach({
                  case (kt, _) =>
                    require(kt subsetOf rset, s"multiple choices: \n1:\n${rset.mkString("\n")}\n2:\n${kt.mkString("\n")}")
                })

              rvalue
            }
          }
        }
    }

    def apply(
      qualifiers: Q*
    ) : V = {
      try {
        get(qualifiers:_*).get
      } catch {
        case ex : Throwable =>
          throw new Exception(s"error getting value for: \n${qualifiers.mkString("\n")}" , ex)

      }
    }
  }

  def Q[Q <: Qualifier](
    qualifiers: Q*
  ) = Qualification(qualifiers:_*)

}
