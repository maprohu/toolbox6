package toolbox6.jartree.impl


trait JarTreeManagement {

  def verifyCache(
    ids: Array[String]
  ) : Array[Int]

  def putCache(
    id: String,
    data: Array[Byte]
  ) : Unit

  def plug(
    jarPluggerClassRequestJson: String,
    param: String
  ) : Array[Byte]

  def query() : String

}



