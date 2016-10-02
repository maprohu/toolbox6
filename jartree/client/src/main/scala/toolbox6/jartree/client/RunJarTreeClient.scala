package toolbox6.jartree.client

import java.rmi.RemoteException

import jartree.util.MavenJarKeyImpl
import org.apache.commons.io.IOUtils
import toolbox6.jartree.framework.HelloByteArray
import toolbox6.jartree.managementapi.{JarTreeManagement, LogListener}
import toolbox6.jartree.managementutils.JarTreeManagementUtils
import toolbox6.jartree.packaging.{JarTreePackaging, JarTreeWarPackager}
import toolbox6.jartree.util.{CaseClassLoaderKey, ManagedJarKeyImpl, RunRequestImpl}
import toolbox6.modules.{JarTreeModules, Toolbox6Modules}
import toolbox6.packaging.MavenHierarchy
import weblogic.jndi.Environment

import scala.collection.immutable._
import scala.io.StdIn

/**
  * Created by martonpapp on 02/10/16.
  */
object RunJarTreeClient {
  def main(args: Array[String]): Unit = {
    val env = new Environment()
    env.setProviderURL("t3://localhost:7002")
    val ctx = env.getInitialContext
    val management =
      ctx
        .lookup(
          JarTreeManagementUtils.bindingName(
            "ftx-core"
          )
        )
        .asInstanceOf[JarTreeManagement]

    println(management.sayHello())

    val cb = new LogListener {
      @throws(classOf[RemoteException])
      override def entry(msg: String): Unit = {
        println(msg)
      }
    }

    val reg = management.registerLogListener(cb)

    val module : MavenHierarchy =
      JarTreeWarPackager.filteredHierarchy(
        JarTreeModules.Framework
      )

    val jars : IndexedSeq[(String, () => Array[Byte])] =
      module
        .jars
        .distinct
        .map({ h =>

          val id = JarTreePackaging.getId(h)
          val data = () => IOUtils.toByteArray(JarTreePackaging.resolveInputStream(h))

          (id.uniqueId, data)
        })
        .toIndexedSeq



    val ids = jars.map(_._1).toArray
    println(s"verifying: ${ids.mkString(", ")}")

    val missingIndices = management.verifyCache(
      ids
    )

    println(s"missing: ${missingIndices.mkString(", ")}")

    missingIndices.foreach({ idx =>
      val (id, data) = jars(idx)
      println(s"uploading: ${id}")
      management.putCache(
        id,
        data()
      )
    })

    println("executing")

    val bytes = management
      .executeByteArray(
        RunRequestImpl.toString(
          RunRequestImpl(
            CaseClassLoaderKey(
              JarTreePackaging.getId(module.jar),
              Seq()
            ),
            classOf[HelloByteArray].getName
          )
        ),
        "client".getBytes
      )

    println(new String(bytes))

    StdIn.readLine("enter...")

    reg.unregister()
  }
}
