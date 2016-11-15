package osgi6.geoserver.geotools

import java.util
import java.util.ResourceBundle
import java.util.logging.{Level, LogManager}

import com.typesafe.scalalogging.LazyLogging
import org.geotools.coverage.processing.CoverageProcessor
import org.geotools.referencing.factory.DeferredAuthorityFactory
import org.geotools.renderer.i18n.Vocabulary
import org.geotools.util.WeakCollectionCleaner
import org.geotools.util.logging.Logging
import org.slf4j.LoggerFactory

/**
  * Created by pappmar on 08/08/2016.
  */
object GeotoolsUtil extends LazyLogging {

  def init() = {
    Logging.getLogger("org.geotools").setLevel(Level.OFF)
  }

  def shutdown() : Unit = {
    WeakCollectionCleaner.DEFAULT.exit()
    DeferredAuthorityFactory.exit()
    ResourceBundle.clearCache(classOf[Vocabulary].getClassLoader)

//    try {
//      logger.info("removing geotools logging levels")
//
//      val knownLevelClass =
//        Option(
//          classOf[Level]
//            .getClassLoader
//        ).orElse(
//          Option(ClassLoader.getSystemClassLoader)
//        ).map({ cl =>
//          cl.loadClass(
//            "java.util.logging.Level$KnownLevel"
//          )
//        }).getOrElse(
//          Class.forName(
//            "java.util.logging.Level$KnownLevel"
//          )
//        )
//
//      logger.info(s"class: ${knownLevelClass} with classLoader: ${knownLevelClass.getClassLoader}")
//
//      import scala.collection.JavaConversions._
//      knownLevelClass.synchronized {
//
//        val levelObjectField = knownLevelClass.getDeclaredField("levelObject")
//        levelObjectField.setAccessible(true)
//
//        def processMap(map: java.util.Map[AnyRef, java.util.List[AnyRef]]) = {
//          val newMap =
//            new util.HashMap[AnyRef, java.util.List[AnyRef]](map)
//              .mapValues[util.List[AnyRef]]({ list =>
//                new util.ArrayList(
//                  new util.ArrayList(list)
//                    .filter({ knownLevel =>
//                      val level = levelObjectField.get(
//                        knownLevel
//                      ).asInstanceOf[Level]
//
//                      if (level eq CoverageProcessor.OPERATION) {
//                        logger.info(s"removing level: ${level}")
//                        false
//                      } else {
//                        logger.info(s"not removing level: ${level} - ${level.getClass}")
//                        true
//                      }
//                    })
//                )
//              })
//
//          map.putAll(newMap)
//
//
////          val outer =
////            map
////              .values()
////              .iterator()
////
////          while (outer.hasNext) {
////            val list = outer.next()
////
////            val it = list.iterator()
////
////            while (it.hasNext) {
////              val knownLevel = it.next()
////
////              val level = levelObjectField.get(
////                knownLevel
////              ).asInstanceOf[Level]
////
////              if (level eq CoverageProcessor.OPERATION) {
////                logger.info(s"removing level: ${level}")
////                it.remove()
////              } else {
////                logger.info(s"not removing level: ${level}")
////              }
////            }
////          }
//
//        }
//
//
//        logger.info("processing intToLevels")
//        processMap(
//          getStaticFieldValue[java.util.Map[AnyRef, java.util.List[AnyRef]]](
//            knownLevelClass,
//            "intToLevels"
//          )
//        )
//
//        logger.info("processing nameToLevels")
//        processMap(
//          getStaticFieldValue[java.util.Map[AnyRef, java.util.List[AnyRef]]](
//            knownLevelClass,
//            "nameToLevels"
//          )
//        )
//      }
//
//      logger.info("cleaning up logging levels done")
//    } catch {
//      case ex : Throwable =>
//        logger.info("Could not unload logger level", ex)
//    }

  }

  def getStaticFieldValue[T <: AnyRef](
    className : String,
    fieldName : String
  ) : T = {
    val clazz = Class.forName(className)
    getStaticFieldValue(clazz, fieldName)
  }

  def getStaticFieldValue[T <: AnyRef](
    clazz : Class[_],
    fieldName : String
  ) : T = {
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(null).asInstanceOf[T]
  }

}
