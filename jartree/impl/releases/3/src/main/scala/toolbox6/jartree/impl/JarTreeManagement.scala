package toolbox6.jartree.impl


trait JarTreeManagement {

  def verifyCache(
    uniqueId: String
  ) : Boolean

  def putCache(
    uniqueId: String,
    data: Array[Byte]
  ) : Unit

  def plug(
    jarPluggerClassRequestJson: String,
    param: Array[Byte]
  ) : Unit

  def query() : String

}



