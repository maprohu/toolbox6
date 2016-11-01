package toolbox6.jartree.managementapi

import toolbox6.jartree.api.PlugRequest
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}

sealed trait Request
sealed trait Response

case class VerifyCache(
  ids: Seq[String]
) extends Request

case class CacheVerified(
  idxs: Seq[Int]
) extends Response

case class PutCache(
  id: String,
  data: Array[Byte]
) extends Request

case class Plug(
  request: PlugRequest[Processor, JarTreeServletContext]
//  classLoader: CaseClassLoaderKey,
//  className: String
) extends Request

case object Done extends Response

case object Query extends Request

case class QueryResult(
  request: Option[PlugRequest[Processor, JarTreeServletContext]],
  webappVersion: String
) extends Response
