package toolbox6.leveldb

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory._

/**
  * Created by maprohu on 02-11-2016.
  */
class LevelDB(
  dbDir: File
) extends LazyLogging {
  val db = {
    logger.info(s"hello db using: ${dbDir}")
    dbDir.mkdirs()
    val options = new Options
    options.createIfMissing(true)

    factory.open(
      dbDir,
      options
    )
  }

  val close = Cancelable(() => db.close())
}

object LevelDB {
  def apply(
    dbDir: File
  ): LevelDB = new LevelDB(dbDir)
}
