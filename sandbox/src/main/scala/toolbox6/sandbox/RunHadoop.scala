package toolbox6.sandbox

import java.io.File

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.DFSConfigKeys
import org.apache.hadoop.hdfs.server.datanode.DataNode
import org.apache.hadoop.hdfs.server.namenode.NameNode
import org.apache.log4j.{BasicConfigurator, Level, Logger}

/**
  * Created by pappmar on 07/02/2017.
  */
object RunHadoop {

  val HadoopHome = "C:/Oracle/wls/hadoop"
  val NameNodePort = 4301
  val NameDir = "name"

  lazy val common = {
    BasicConfigurator.configure()

    Logger
      .getRootLogger
      .setLevel(Level.INFO)

    System.setProperty("hadoop.home.dir", HadoopHome)
  }

  def localDirFile(path: String) = {
    new File(s"../toolbox6/local/hadoop/${path}")
  }

  def localDir(path: String) = {
    val dirFile = localDirFile(path)
    dirFile.mkdirs()
    dirFile.getAbsoluteFile.getCanonicalFile.toURI.toASCIIString
  }

  lazy val cfg = {
    common

    val cfg = new Configuration()


    cfg.set(
      FileSystem.FS_DEFAULT_NAME_KEY,
      s"hdfs://localhost:${NameNodePort}"
    )
    cfg.set(
      DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
      localDir(NameDir)
    )
    cfg

  }

}

object RunHadoopFormat {

  def main(args: Array[String]): Unit = {

    NameNode.format(
      RunHadoop.cfg
    )

  }
}

object RunHadoopNameNode {
  def main(args: Array[String]): Unit = {
    NameNode.createNameNode(
      args,
      RunHadoop.cfg
    )
  }
}

object RunDataNode {
  def setup(name: String) = {
    RunHadoop.common

    val cfg = new Configuration()

    cfg.set(
      DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY,
      s"localhost:${RunHadoop.NameNodePort}"
    )
    cfg.set(
      DFSConfigKeys.DFS_DATANODE_DATA_DIR_KEY,
      RunHadoop.localDir(name)
    )

    cfg
  }
}

object RunDataNode01 {
  def main(args: Array[String]): Unit = {
    val cfg = RunDataNode.setup("data01")

    DataNode.createDataNode(
      args,
      cfg
    )

  }
}

object RunDataNode02 {

  val Port = 50011
  val InfoPort = 50076
  val IpcPort = 50021

  def main(args: Array[String]): Unit = {
    val cfg = RunDataNode.setup("data02")

    cfg.set(
      DFSConfigKeys.DFS_DATANODE_ADDRESS_KEY,
      s"0.0.0.0:${Port}"
    )
    cfg.set(
      DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_KEY,
      s"0.0.0.0:${IpcPort}"
    )
    cfg.set(
      DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_KEY,
      s"0.0.0.0:${InfoPort}"
    )

    DataNode.createDataNode(
      args,
      cfg
    )

  }
}
