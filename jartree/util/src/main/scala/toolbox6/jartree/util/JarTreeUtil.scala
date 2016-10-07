package toolbox6.jartree.util

import java.io.{File, FileInputStream, InputStream, OutputStream}
import java.security.{DigestInputStream, MessageDigest}
import java.util
import javax.json.JsonObject

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import toolbox6.jartree.api._
import upickle.Js
import upickle.Js.Obj

import scala.collection.immutable._
import scala.collection.JavaConversions._

/**
  * Created by pappmar on 31/08/2016.
  */

//sealed trait CaseJarKey extends JarKey

case class CaseJarKey(
  uniqueId: String
) extends JarKey

object CaseJarKey {
  def apply(
    jarKey: JarKey
  ) : CaseJarKey = {
    CaseJarKey(
      jarKey.uniqueId()
    )
  }

  def apply(
    file: File
  ) : CaseJarKey = {
    apply(() => new FileInputStream(file))
  }

  def apply(
    bytes: () => InputStream
  ) : CaseJarKey = {
    apply(
      hashToString(
        calculateHash(
          bytes()
        )
      )
    )
  }

  def createDigest = {
    MessageDigest.getInstance("SHA-256")
  }

  def createDigestInputStream(is: InputStream) = {
    new DigestInputStream(is, createDigest)
  }

  def calculateHash(in: InputStream) : Array[Byte] = {
    try {
      val digestInputStream = createDigestInputStream(in)
      IOUtils.copy(digestInputStream, new OutputStream {
        override def write(i: Int): Unit = ()
      })
      digestInputStream.getMessageDigest.digest()
    } finally {
      IOUtils.closeQuietly(in)
    }
  }

  def hashToString(hash: Array[Byte]) : String = {
    Base64.encodeBase64URLSafeString(hash)
  }


}

case class CaseClassLoaderKey(
  jarsSeq: Seq[CaseJarKey],
  parentOpt: Option[CaseClassLoaderKey]
) extends ClassLoaderKey {
  override val jars: util.Collection[JarKey] = jarsSeq
  override val parent: ClassLoaderKey = parentOpt.orNull
}

object CaseClassLoaderKey {
  def apply(clk: ClassLoaderKey) : CaseClassLoaderKey = apply(
    jarsSeq = clk.jars().to[Seq].map(CaseJarKey.apply),
    parentOpt = Option(CaseClassLoaderKey(clk.parent()))
  )
}

case class ClassRequestImpl[+T](
  classLoader: CaseClassLoaderKey,
  className: String
) extends ClassRequest[T]

object ClassRequestImpl {
  def fromString[T](str: String) : ClassRequestImpl[T] = {
    upickle.default.read[ClassRequestImpl[Any]](str)
      .asInstanceOf[ClassRequestImpl[T]]
  }

  def fromJavax[T](o: JsonObject) : ClassRequestImpl[T] = {
    upickle.default.readJs[ClassRequestImpl[Any]](
      JsonTools.fromJavax(o)
    ).asInstanceOf[ClassRequestImpl[T]]
  }

  def toString[T](req: ClassRequestImpl[T]) : String = {
    upickle.default.write(req, 2)
  }

  def toJsObj[T](req: ClassRequestImpl[T]) : Js.Obj = {
    upickle.default.writeJs(req).asInstanceOf[Js.Obj]
  }

  def apply[T](req: ClassRequest[T]) : ClassRequestImpl[T] = {
    apply[T](
      req.classLoader(),
      req.className()
    )
  }

  def apply[T](
    classLoaderKey: ClassLoaderKey,
    className: String
  ) : ClassRequestImpl[T] = ClassRequestImpl(CaseClassLoaderKey(classLoaderKey), className)
}