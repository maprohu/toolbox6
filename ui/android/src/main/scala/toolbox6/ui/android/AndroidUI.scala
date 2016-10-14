package toolbox6.ui.android

import android.app.Activity
import android.view.View.OnClickListener
import android.view.{View, ViewGroup}
import android.widget.{Button, LinearLayout}
import toolbox6.ui.ast

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
  * Created by pappmar on 14/10/2016.
  */
class AndroidUI(activity: Activity) extends ast.UI {
  override def displaySync(widget: ast.Widget): Unit = {
    val p = new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    activity.setContentView(
      create(widget),
      p
    )
  }

  def create(widget: ast.Widget) : View = {
    widget match {
      case w : ast.Button =>
        val o = new Button(activity)
        o.setText(w.label)
        o.setSelected(w.position)
        o.setEnabled(w.ability)
        o.setOnClickListener(
          new OnClickListener {
            override def onClick(v: View): Unit = {
              w.click.simple()
            }
          }
        )
        o
      case w : ast.Column[ast.Widget] =>
        val panel = new LinearLayout(activity)
        panel.setOrientation(LinearLayout.VERTICAL)

        w.widgets.zipWithIndex.foreach({
          case (sw, idx) =>

            val c = new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT,
              1
            )
            panel.addView(create(sw.widget), c)
        })

        panel


    }

  }

  override def run[T](fn: => T): Future[T] = {
    val promise = Promise[T]()
    activity.runOnUiThread(
      new Runnable {
        override def run(): Unit = {
          promise.complete(Try(fn))
        }
      }
    )
    promise.future
  }
}
