package toolbox6.jartree.managementapi

import java.rmi.RemoteException


trait JarTreeManagement extends java.rmi.Remote {

//  @throws(classOf[RemoteException])
//  def sayHello() : String
//
//  @throws(classOf[RemoteException])
//  def registerLogListener(listener: LogListener) : Registration

  @throws(classOf[RemoteException])
  def verifyCache(
    ids: Array[String]
  ) : Array[Int]

  @throws(classOf[RemoteException])
  def putCache(
    id: String,
    data: Array[Byte]
  ) : Unit

  @throws(classOf[RemoteException])
  def plug(
    plugRequest: Array[Byte]
  ) : Array[Byte]

  @throws(classOf[RemoteException])
  def query() : Array[Byte]

//  @throws(classOf[RemoteException])
//  def executeByteArray(
//    runRequestImplJson: String,
//    input: Array[Byte]
//  ) : Array[Byte]

}

//trait LogListener extends java.rmi.Remote {
//
//  @throws(classOf[RemoteException])
//  def entry(msg: String)
//
//}

//trait Registration extends java.rmi.Remote {
//
//  @throws(classOf[RemoteException])
//  def unregister() : Unit
//
//}


