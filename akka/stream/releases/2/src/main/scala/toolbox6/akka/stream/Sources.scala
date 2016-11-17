package toolbox6.akka.stream

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}


object Sources {


  def singleMaterializedValue[T](fn: () => T) = {
    Source.fromGraph(new SingleMaterializedValueStage[T](fn))
  }

  class SingleMaterializedValueStage[T](fn: () => T) extends GraphStageWithMaterializedValue[SourceShape[T], T] {

    val out = Outlet[T]("single.out")
    val shape = SourceShape(out)

    @scala.throws[Exception](classOf[Exception])
    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, T) = {
      val elem = fn()

      val logic = new GraphStageLogic(shape) with OutHandler {
        def onPull(): Unit = {
          push(out, elem)
          completeStage()
        }
        setHandler(out, this)
      }

      (logic, elem)
    }

  }

}