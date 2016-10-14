package toolbox6.ui.android

import android.app.Activity
import android.view.{View, ViewGroup}
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import toolbox6.ui.ast

/**
  * Created by pappmar on 14/10/2016.
  */
class AndroidUI(activity: Activity) extends ast.UI {
  override def display(widget: ast.Widget): Unit = {
    val layout = new LinearLayout(activity)
    layout.setOrientation(LinearLayout.VERTICAL)
    val p = new LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
      1
    )
    layout.addView(
      create(widget),
      p
    )
    activity.setContentView(layout)
  }

  def create(widget: ast.Widget) : View = {
    ???

  }
}
