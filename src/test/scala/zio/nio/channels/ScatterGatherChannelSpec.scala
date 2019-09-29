package zio.nio.channels

import java.nio.file.StandardOpenOption

import zio.nio.file.{ Files, Path }
import zio.nio.{ BaseSpec, Buffer }
import zio.test.Assertion._
import zio.test._
import zio.{ Chunk, IO }

import scala.io.Source

object ScatterGatherChannelSpec
    extends BaseSpec(
      suite("ScatterGatherChannelSpec")(
        testM("scattering read") {
          val testFilePath = Path("src") / "test" / "resources" / "scattering_read_test.txt"
          def readLine(buffer: Buffer[Byte]) =
            for {
              _     <- buffer.flip
              chunk <- buffer.getChunk()
            } yield chunk.takeWhile(_ != 10).map(_.toChar).mkString.trim
          FileChannel.open(testFilePath, StandardOpenOption.READ).use { channel =>
            for {
              buffs <- IO.collectAll(Seq(Buffer.byte(5), Buffer.byte(5)))
              _     <- channel.readBuffer(buffs)
              list  <- IO.traverse(buffs)(readLine)
            } yield assert(list == "Hello" :: "World" :: Nil, isTrue)
          }
        },
        testM("gathering write") {
          val testFilePath = Path("target") / "test-data" / "gathering_write_test.txt"
          val openChannel = for {
            _       <- Files.createDirectories(testFilePath.parent.get).toManaged_
            channel <- FileChannel.open(testFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
          } yield channel
          openChannel.use { channel =>
            for {
              buffs <- IO.collectAll(
                        Seq(
                          Buffer.byte(Chunk.fromArray("Hello".getBytes)),
                          Buffer.byte(Chunk.fromArray("World".getBytes))
                        )
                      )
              _      <- channel.writeBuffer(buffs)
              result <- IO.effect(Source.fromFile(testFilePath.toFile).getLines.toSeq)
              _      <- Files.delete(testFilePath)
            } yield assert(result == Seq("HelloWorld"), isTrue)
          }
        }
      )
    )
