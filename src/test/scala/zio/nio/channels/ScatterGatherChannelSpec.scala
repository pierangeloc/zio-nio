package zio.nio.channels

import java.io.{ File, RandomAccessFile }

import scala.io.Source

import zio.test._
import zio.test.Assertion._
import zio.nio.{ BaseSpec, Buffer }
import zio.{ Chunk, IO, ZIO }

object ScatterGatherChannelSpec
    extends BaseSpec(
      suite("ScatterGatherChannelSpec")(
        testM("scattering read") {
          for {
            raf         <- ZIO.effectTotal(new RandomAccessFile("src/test/resources/scattering_read_test.txt", "r"))
            fileChannel = raf.getChannel
            readLine = (buffer: Buffer[Byte]) =>
              for {
                _     <- buffer.flip
                array <- buffer.array
                text  = array.takeWhile(_ != 10).map(_.toChar).mkString.trim
              } yield text
            buffs <- IO.collectAll(Seq(Buffer.byte(5), Buffer.byte(5)))
            list <- FileChannel(fileChannel).use { channel =>
                     for {
                       _    <- channel.readBuffer(buffs)
                       list <- IO.collectAll(buffs.map(readLine))
                     } yield list
                   }
          } yield assert(list == "Hello" :: "World" :: Nil, isTrue)
        },
        testM("gathering write") {
          for {
            file        <- ZIO.effect(new File("src/test/resources/gathering_write_test.txt"))
            raf         = new RandomAccessFile(file, "rw")
            fileChannel = raf.getChannel

            buffs <- IO.collectAll(
                      Seq(
                        Buffer.byte(Chunk.fromArray("Hello".getBytes)),
                        Buffer.byte(Chunk.fromArray("World".getBytes))
                      )
                    )
            _ <- FileChannel(fileChannel).use { channel =>
                  for {
                    _ <- channel.writeBuffer(buffs)
                  } yield ()
                }

            result = Source.fromFile(file).getLines.toSeq
            _      = file.delete()
          } yield assert(result == Seq("HelloWorld"), isTrue)
        }
      )
    )
