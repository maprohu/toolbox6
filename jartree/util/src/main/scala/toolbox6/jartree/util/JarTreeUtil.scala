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

sealed trait CaseJarKey extends JarKey

object CaseJarKey {
  def apply(
    jarKey: JarKey
  ) : CaseJarKey = {
    jarKey match {
//      case k : HashJarKey =>
//        HashJarKeyImpl(
//          k.hash().to[Seq]
//        )
//      case k : MavenJarKey =>
//        MavenJarKeyImpl(
//          k.groupId(),
//          k.artifactId(),
//          k.version(),
//          Option(k.classifier()).filterNot(_.isEmpty)
//        )
      case k : ManagedJarKey =>
        ManagedJarKeyImpl(
          k.uniqueId()
        )
      case _ => ???
    }
  }
}

case class ManagedJarKeyImpl(
  uniqueId: String
) extends ManagedJarKey with CaseJarKey

object ManagedJarKeyImpl {
  def apply(
    file: File
  ) : ManagedJarKeyImpl = {
    apply(() => new FileInputStream(file))
  }

  def apply(
    bytes: () => InputStream
  ) : ManagedJarKeyImpl = {
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

//case class HashJarKeyImpl(
//  hashSeq : Seq[Byte]
//) extends CaseJarKey with HashJarKey {
//  override val hash: Array[Byte] = hashSeq.toArray
//}

//case class MavenJarKeyImpl(
//  groupId: String,
//  artifactId: String,
//  version: String,
//  classifierOpt: Option[String]
//) extends CaseJarKey with MavenJarKey {
//  override def classifier(): String = classifierOpt.getOrElse("")
//}
//
//object MavenJarKeyImpl {
//
//  def apply(canonical: String) : MavenJarKeyImpl = {
//    val mc = MavenCoordinates.createCoordinate(canonical)
//
//    MavenJarKeyImpl(
//      mc.getGroupId,
//      mc.getArtifactId,
//      mc.getVersion,
//      Option(mc.getClassifier).filterNot(_.isEmpty)
//    )
//  }
//
//}

//case class FileJarKeyImpl(
//  file: File
//) extends CaseJarKey


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