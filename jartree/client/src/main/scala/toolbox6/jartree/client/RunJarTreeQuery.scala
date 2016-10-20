package toolbox6.jartree.client

import java.nio.ByteBuffer

import toolbox6.jartree.managementapi.JarTreeManagement
import toolbox6.jartree.managementutils.{JarTreeManagementUtils, QueryResult}
import weblogic.jndi.Environment


/**
  * Created by martonpapp on 02/10/16.
  */
object RunJarTreeQuery {
  val Target = "t3://localhost:7002"
  val App = "ftx-core"

  def main(args: Array[String]): Unit = {
    val env = new Environment()
    env.setProviderURL(Target)
    val ctx = env.getInitialContext
    val management =
      ctx
        .lookup(
          JarTreeManagementUtils.bindingName(
            App
          )
        )
        .asInstanceOf[JarTreeManagement]


    import boopickle.Default._

    val q = Unpickle[QueryResult]
      .fromBytes(
        ByteBuffer.wrap(
          management.query()
        )
      )


    println(
      q
    )

//    val cb = new LogListener {
//      @throws(classOf[RemoteException])
//      override def entry(msg: String): Unit = {
//        println(msg)
//      }
//    }
//
//    val reg = management.registerLogListener(cb)
//
//    val module : MavenHierarchy =
//      JarTreeWarPackager.filteredHierarchy(
//        JarTreeModules.Framework
//      )
//
//    val jars : IndexedSeq[(String, () => Array[Byte])] =
//      module
//        .jars
//        .distinct
//        .map({ h =>
//
//          val id = JarTreePackaging.getId(h)
//          val data = () => IOUtils.toByteArray(JarTreePackaging.resolveInputStream(h))
//
//          (id.uniqueId, data)
//        })
//        .toIndexedSeq
//
//
//
//    val ids = jars.map(_._1).toArray
//    println(s"verifying: ${ids.mkString(", ")}")
//
//    val missingIndices = management.verifyCache(
//      ids
//    )
//
//    println(s"missing: ${missingIndices.mkString(", ")}")
//
//    missingIndices.foreach({ idx =>
//      val (id, data) = jars(idx)
//      println(s"uploading: ${id}")
//      management.putCache(
//        id,
//        data()
//      )
//    })
//
//    println("executing")
//
//    val bytes = management
//      .executeByteArray(
//        ClassRequestImpl.toString(
//          ClassRequestImpl(
//            CaseClassLoaderKey(
//              JarTreePackaging.getId(module.jar),
//              Seq()
//            ),
//            classOf[HelloByteArray].getName
//          )
//        ),
//        "client".getBytes
//      )
//
//    println(new String(bytes))
//
//    StdIn.readLine("enter...")
//
//    reg.unregister()
  }
}
