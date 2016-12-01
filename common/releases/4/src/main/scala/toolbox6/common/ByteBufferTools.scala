package toolbox6.common

import java.io.{File, FileOutputStream, InputStream, RandomAccessFile}
import java.nio.ByteBuffer

import org.apache.commons.io.IOUtils

/**
  * Created by pappmar on 17/10/2016.
  */
object ByteBufferTools {

  def readFile(file: File) : ByteBuffer = {
    val raf = new RandomAccessFile(file, "r")
    try {
      val channel = raf.getChannel
      try {
        val size = channel.size()
        val buffer = ByteBuffer.allocate(size.toInt)
        channel.read(buffer)
        buffer.flip()
        buffer
      } finally {
        channel.close()
      }
    } finally {
      raf.close()
    }
  }

  def readInputStream(isp: () => InputStream) : ByteBuffer = {
    val is = isp()
    try {
      ByteBuffer.wrap(IOUtils.toByteArray(is))
    } finally {
      is.close()
    }
  }

//  def writeFile(buffers: Array[ByteBuffer], file: File) : Unit = {
//    val channel = new FileOutputStream(file).getChannel
//    try {
//      channel.write(buffers)
//    } finally {
//      channel.close()
//    }
//  }

  def writeFile(buffer: ByteBuffer, file: File) : Unit = {
    val channel = new FileOutputStream(file).getChannel
    try {
      channel.write(buffer)
    } finally {
      channel.close()
    }
  }


}
