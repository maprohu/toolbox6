package toolbox6.ui.swing

import java.awt.Frame
import javax.swing.JFrame

/**
  * Created by pappmar on 14/10/2016.
  */
object RunSwingUi {

  def main(args: Array[String]): Unit = {

    val frame = new JFrame()
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setExtendedState(
      Frame.MAXIMIZED_BOTH
    )
    frame.pack()
    frame.setVisible(true)

  }

}
