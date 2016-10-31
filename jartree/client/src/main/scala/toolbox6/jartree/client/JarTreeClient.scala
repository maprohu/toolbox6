package toolbox6.jartree.client

import java.rmi.RemoteException

import maven.modules.builder.NamedModule
import org.apache.commons.io.IOUtils
import toolbox6.jartree.packaging.JarTreePackaging.{RunHierarchy, RunMavenHierarchy}
import toolbox6.jartree.packaging.{JarTreePackaging, JarTreeWarPackager}
import toolbox6.jartree.util.{CaseClassLoaderKey, ClassRequestImpl, JsonTools}
import toolbox6.modules.JarTreeModules
import toolbox6.packaging.MavenHierarchy
import upickle.Js
import weblogic.jndi.Environment

import scala.collection.immutable._

/**
  * Created by martonpapp on 02/10/16.
  */
//object JarTreeClient {
//
//
//
//
//  def deploy(
//    adminUrl: String = "t3://localhost:7003",
//    app: String,
//    runHierarchy: RunHierarchy
//  ) : Unit = {
//    val env = new Environment()
//    env.setProviderURL(adminUrl)
//    val ctx = env.getInitialContext
//    val management =
//      ctx
//        .lookup(
//          JarTreeManagementUtils.bindingName(
//            app
//          )
//        )
//        .asInstanceOf[JarTreeManagement]
//
////    println(management.sayHello())
//
////    val cb = new LogListener {
////      @throws(classOf[RemoteException])
////      override def entry(msg: String): Unit = {
////        println(msg)
////      }
////    }
//
////    val reg = management.registerLogListener(cb)
//
//    val runMavenHierarchy : RunMavenHierarchy = ???
//
//    val jars = JarTreePackaging.resolverJars(runMavenHierarchy)
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
//      .plug(
//        ???
////        ClassRequestImpl.toString(
////          runMavenHierarchy.request
////        ),
////        upickle.json.write(
////          runMavenHierarchy.childrenJs,
////          2
////        )
//      )
//
//    println(new String(bytes))
//
////    reg.unregister()
//  }
//
//


//}
