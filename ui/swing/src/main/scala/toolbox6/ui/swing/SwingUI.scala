package toolbox6.ui.swing

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.{JFrame, JPanel, JToggleButton, SwingUtilities}

import toolbox6.ui.ast
import toolbox6.ui.ast.Column

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
  * Created by pappmar on 14/10/2016.
  */
class SwingUI(container: Container) extends ast.UI {
  override def displaySync(widget: ast.Widget): Unit = {
    container.removeAll()
    container.setLayout(new GridBagLayout)
    val c = new GridBagConstraints()
    c.gridy = 0
    c.gridx = 0
    c.weightx = 1
    c.weighty = 1
    c.fill = GridBagConstraints.BOTH
    container.add(
      create(widget),
      c
    )
    container.revalidate()
  }


  def create(widget: ast.Widget) : Component = {
    widget match {
      case w : ast.Button =>
        val o = new JToggleButton()
        o.setText(w.label)
        o.setSelected(w.position)
        o.setEnabled(w.ability)
        o.addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent): Unit = {
            w.click.simple()
          }
        })
        o
      case w : ast.Column[ast.Widget] =>
        val panel = new JPanel(new GridBagLayout())

        w.widgets.zipWithIndex.foreach({
          case (sw, idx) =>
            val c = new GridBagConstraints()
            c.fill = GridBagConstraints.BOTH
            c.gridy = idx
            c.gridx = 0
            c.weightx = 1
            c.weighty = 1
            panel.add(create(sw.widget), c)
        })

        panel


    }

  }

  override def run[T](fn: => T): Future[T] = {
    val promise = Promise[T]()
    SwingUtilities.invokeLater(new Runnable {
      override def run(): Unit = {
        promise.complete(Try(fn))
      }
    })
    promise.future
  }
}

object SwingUI {
  def fullScreen(w: ast.UI => Unit): Unit = {
    val frame = new JFrame()
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setExtendedState(
      Frame.MAXIMIZED_BOTH
    )

    val ui = new SwingUI(frame.getContentPane)
    w(ui)

    frame.pack()
    frame.setVisible(true)

  }
}
