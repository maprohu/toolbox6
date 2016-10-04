package toolbox6.jartree.util

import java.io.{File, FileInputStream, InputStream, OutputStream}
import java.security.{DigestInputStream, MessageDigest}
import java.util

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import toolbox6.jartree.api._

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
  jar: CaseJarKey,
  dependenciesSeq: Seq[CaseClassLoaderKey]
) extends ClassLoaderKey {
  override def dependencies(): util.Collection[ClassLoaderKey] = dependenciesSeq
}

object CaseClassLoaderKey {
  def apply(clk: ClassLoaderKey) : CaseClassLoaderKey = apply(
    CaseJarKey(clk.jar),
    clk.dependencies.map(clk => CaseClassLoaderKey(clk)).to[Seq]
  )
}

case class RunRequestImpl(
  classLoader: CaseClassLoaderKey,
  className: String
) extends RunRequest

object RunRequestImpl {
  def fromString(str: String) : RunRequestImpl = {
    upickle.default.read[RunRequestImpl](str)
  }

  def toString(req: RunRequestImpl) : String = {
    upickle.default.write(req, 2)
  }

  def apply(req: RunRequest) : RunRequestImpl = {
    apply(
      req.classLoader(),
      req.className()
    )
  }

  def apply(
    classLoaderKey: ClassLoaderKey,
    className: String
  ) : RunRequestImpl = RunRequestImpl(CaseClassLoaderKey(classLoaderKey), className)
}