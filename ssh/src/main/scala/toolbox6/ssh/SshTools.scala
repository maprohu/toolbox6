package toolbox6.ssh

import java.io._

import ammonite.ops.Path
import com.jcraft.jsch._
import com.typesafe.scalalogging.StrictLogging
import toolbox6.logging.LogTools

import scala.concurrent.duration._

/**
  * Created by martonpapp on 15/10/16.
  */
object SshTools extends StrictLogging with LogTools  {

//  val ServerAliveInterval = 2.seconds
  val ServerAliveInterval = 120.seconds
  val ServerAliveCountMax = 2

  trait Config {
    def user: String
    def host: String
    def sshPort: Int
    def key : Path
    def hostKey: Array[Byte]
  }

  case class ConfigImpl(
    user: String,
    host: String,
    sshPort: Int,
    key: Path,
    hostKey: Array[Byte]
  ) extends Config

  def connect(implicit
    config: Config
  ) = {
    logger.info(s"connecting to: ssh://${config.user}@${config.host}:${config.sshPort}")
    val jsch = new JSch
    jsch.addIdentity(config.key.toString())
    val session = jsch.getSession(config.user, config.host, config.sshPort)
    session.setUserInfo(AcceptNoneUserInfo)
    session.setServerAliveInterval(ServerAliveInterval.toMillis.toInt)
    session.setServerAliveCountMax(ServerAliveCountMax)

    session.setHostKeyRepository(
      new SingleHost(config.hostKey)
    )
    session.connect()
    session
  }

  def run(
    cmd: String
  )(implicit
    config: Config
  ) = {
    implicit val session = connect(config)
    val es = command(cmd)
    println(s"\n--------------\nexit-status: ${es}")
  }

  def command(
    cmd: String
  )(implicit
    session: Session
  ) : (Int, Array[Byte], Array[Byte])= {
    logger.info(s"running: ${cmd}")
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    try {
      channel.setCommand(cmd)
      channel.setInputStream(null)
      val ebos = new ByteArrayOutputStream()
      channel.setErrStream(ebos)
      val in = channel.getInputStream
      channel.connect()

      val bos = new ByteArrayOutputStream()
      copy(in, bos)
      val ba = bos.toByteArray
      //      System.out.write(ba)

      (channel.getExitStatus, ba, ebos.toByteArray)
    } finally {
      channel.disconnect()
    }
  }

  def commandInteractive(
    cmd: String
  )(implicit
    session: Session
  ) : Int = {
    logger.info(s"running: ${cmd}")
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    try {
      channel.setCommand(cmd)
      channel.setInputStream(null)
      channel.setErrStream(System.err)
      val in = channel.getInputStream
      channel.connect()

      copy(in, System.out)

      channel.getExitStatus
    } finally {
      channel.disconnect()
    }
  }

  def execValue[T](
    cmd: String,
    proc: (ChannelExec) => T
  )(implicit
    session: Session
  ) : T = {
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    try {
      logger.info(s"exec: ${cmd}")
      channel.setCommand(cmd)
      channel.connect()
      proc(channel)
    } finally {
      channel.disconnect()
    }
  }

  def exec(
    cmd: String,
    proc: (Channel, InputStream, OutputStream) => Unit
  )(implicit
    session: Session
  ) = {
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    try {
      logger.info(s"exec: ${cmd}")
      channel.setCommand(cmd)
      val out = channel.getOutputStream
      val in = channel.getInputStream
      channel.connect()

      proc(channel, in, out)

      channel.getExitStatus
    } finally {
      channel.disconnect()
    }

  }

  def copy(
    from: InputStream,
    to: OutputStream
  ) = {
    val buff = Array.ofDim[Byte](1024)
    Iterator
      .continually(from.read(buff))
      .takeWhile(_ != -1)
      .map(buff.take)
      .foreach(to.write)
    to.flush()
  }



  def scp(
    from: File,
    to: String
  )(implicit
    session: Session
  ) = {
    scpStream(
      () => new FileInputStream(from),
      from.length(),
      to
    )
  }

  def scpStream(
    from: () => InputStream,
    length: Long,
    to: String
  )(implicit
    session: Session
  ) = {
    exec(
      s"scp -t ${to}",
      { (ch, in, out) =>
        def check = {
          logger.info(s"scp check: ${in.read()}")
        }
        check

        val cmd = s"C0644 ${length} ${to.reverse.takeWhile(_ != '/').reverse}\n"
        logger.info(s"scp cmd: ${cmd}")
        out.write(cmd.getBytes)
        out.flush()
        check
        val fis = from()
        copy(fis, out)
        fis.close()
        out.write(0)
        out.flush()
        check
        out.close()

        in.close()
      }
    )


  }

  val AcceptAllUserInfo = new UserInfo {
    override def promptPassword(message: String): Boolean = ???

    override def promptYesNo(message: String): Boolean = true

    override def showMessage(message: String): Unit = ???

    override def getPassword: String = ???

    override def promptPassphrase(message: String): Boolean = ???

    override def getPassphrase: String = ???
  }

  val AcceptNoneUserInfo = new UserInfo {
    override def promptPassword(message: String): Boolean = ???

    override def promptYesNo(message: String): Boolean = false

    override def showMessage(message: String): Unit = ???

    override def getPassword: String = ???

    override def promptPassphrase(message: String): Boolean = ???

    override def getPassphrase: String = ???
  }

  def tunnel(
    forwardPort: Int,
    reversePort: Int
  )(implicit
    target: Config
  ) = {
    implicit val session = connect

    println(s"localhost:${forwardPort} -> ${target.host}")
    session.setPortForwardingL(
      forwardPort,
      "localhost",
      forwardPort
    )

    println(s"localhost:${reversePort} <- ${target.host}")
    session.setPortForwardingR(
      reversePort,
      "localhost",
      reversePort
    )

  }

  case class ForwardTunnel(
    localPort: Int,
    remoteHost: String = "localhost",
    remotePort: Int
  )
  object ForwardTunnel {
    implicit def apply(
      port: Int
    ): ForwardTunnel = ForwardTunnel(
      localPort = port,
      remotePort = port
    )
  }
  case class ReverseTunnel(
    remotePort: Int,
    localHost: String = "localhost",
    localPort: Int
  )
  object ReverseTunnel {
    implicit def apply(
      port: Int
    ): ReverseTunnel = new ReverseTunnel(
      remotePort = port,
      localPort = port
    )
  }

  def tunnels(
    forward: Seq[ForwardTunnel] = Seq.empty,
    reverse: Seq[ReverseTunnel] = Seq.empty
  )(implicit
    target: Config
  ) = {
    implicit val session = connect

    try {
      forward
        .foreach({ f =>
          import f._
          logger.info(s"localhost:${localPort} -> (${target.host}:${target.sshPort}) -> ${remoteHost}:${remotePort}")
          session.setPortForwardingL(
            localPort,
            remoteHost,
            remotePort
          )
        })

      reverse.foreach({ r =>
        import r._
        logger.info(s"${localHost}:${localPort} <- (${target.host}:${target.sshPort}) <- localhost:${remotePort}")
        session.setPortForwardingR(
          remotePort,
          localHost,
          localPort
        )
      })
    } catch {
      case ex : JSchException =>
        logger.warn(s"port forwarding failed, disconnecting: ${ex.getMessage}")
        quietly { session.disconnect() }
        throw ex
    }

    session
  }

  class SingleHost(
    knownKey: Array[Byte]
  ) extends HostKeyRepository {
    override def getKnownHostsRepositoryID: String = null

    override def remove(host: String, `type`: String): Unit = ???

    override def remove(host: String, `type`: String, key: Array[Byte]): Unit = ()

    override def getHostKey: Array[HostKey] = ???

    override def getHostKey(host: String, `type`: String): Array[HostKey] = {
      Array()
    }

    override def add(hostkey: HostKey, ui: UserInfo): Unit = ()

    override def check(host: String, key: Array[Byte]): Int = {
      if (key sameElements knownKey) {
        HostKeyRepository.OK
      } else {
        logger.warn(s"unkown host key: ${key.toSeq}")
        HostKeyRepository.CHANGED
      }
    }
  }

}
