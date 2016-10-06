package toolbox6.jartree.util

import java.io.{File, FileInputStream, StringReader}
import javax.json._
import javax.json.spi.JsonProvider

import upickle.Js

import scala.collection.JavaConversions._
import scala.collection.immutable._

/**
  * Created by Student on 06/10/2016.
  */
object JsonTools {

  def fromJavax(
    o: JsonObject
  ) : Js.Value = {
    fromNullSafeJavax(
      o,
      fromNonNullJavax(o)
    )
  }

  def fromNonNullJavax(
    o: JsonObject
  ) : Js.Value = {
    Js.Obj(
      o
        .toSeq
        .map({
          case (key, value) =>
            key -> fromJavax(value)
        }):_*
    )
  }

  def fromNullSafeJavax[I](v: I, fn: => Js.Value) : Js.Value = {
    if (v == null) {
      Js.Null
    } else {
      fn
    }
  }

  def fromJavax(
    o: JsonValue
  ) : Js.Value = {
    fromNullSafeJavax(
      o,
      o match {
        case v : JsonNumber =>
          Js.Num(v.doubleValue())
        case v : JsonString =>
          Js.Str(v.getString)
        case v : JsonObject =>
          fromNonNullJavax(v)
        case v : JsonArray =>
          Js.Arr(
            v
              .getValuesAs(classOf[JsonValue])
              .map(fromJavax):_*
          )

      }
    )

  }

  val RequestAttribute = "request"
  val ParamAttribute = "param"

  def readJavax(file: File) : JsonObject = {
    val parser = JsonProvider.provider().createReader(
      new FileInputStream(file)
    )

    try {
      parser.readObject()
    } finally {
      parser.close()
    }
  }

  def readJavax(str: String) : JsonObject = {
    val parser = JsonProvider.provider().createReader(
      new StringReader(str)
    )

    try {
      parser.readObject()
    } finally {
      parser.close()
    }
  }

  def readUpdate[T](o: JsonObject) : (ClassRequestImpl[T], JsonObject) = {
    (
      ClassRequestImpl.fromJavax[T](o.getJsonObject(RequestAttribute)),
      o.getJsonObject(ParamAttribute)
    )
  }
}
