package shopliftr

import zio._
import zio.stream._
import shopliftr.adl.adlService._
import shopliftr.adl.adlService.AdlStream
import shopliftr.adl.adlService.AdlStream
import zio.console._
import shopliftr._
import shopliftr.config._
import shopliftr.adl.adlService._
import shopliftr.adl._
import zio.duration._
import java.io.IOException
import zio.kafka._
import zio.kafka.producer._
import zio.kafka.consumer._
import zio.kafka.serde._
import zio.blocking._
import org.apache.kafka.clients.producer.ProducerRecord

object AdlJobApp extends zio.App {

  val producerSettings: ProducerSettings =
    new ProducerSettings(List("localhost:9092"), 10.seconds, Map())
      .withProperty("linger.ms", "25")
      .withProperty("batch.size", "1000")
      .withProperty("max.request.size", "1000000")
      .withProperty("retries", "10")

  val producer: ZLayer[Any, Throwable, Producer[Any, Long, Array[Byte]]] =
    ZLayer.fromManaged(
      Producer.make(producerSettings, Serde.long, Serde.byteArray)
    )

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    (for {
      fiber <- adlToElasticApp.fork.ensuring(putStrLn("Done sending"))
      _ <- fiber.join
      _ <- putStrLn("The end")
    } yield ())
      .provideLayer(
        (Configuration.live >>> adlService.live) ++ Blocking.live ++ console.Console.live ++ producer
      )
      .exitCode
  }

  val adlToElasticApp: ZIO[
    AdlStream with Console with Blocking with Producer[Any, Long, Array[Byte]],
    Throwable,
    Unit
  ] = (for {
    _ <-
      adlService
        .streamFile(
          "digital-tech-id-map/cardlink/test_data/10_15_2020_jaro_testing/cardlink-sns.csv"
        )
        .grouped(4096)
        .map(byte => new ProducerRecord("offerstealer", 1L, byte.toArray))
        .mapChunksM { record =>
          Producer.produceChunkAsync[Any, Long, Array[Byte]](record).flatten
        }

  } yield ()).runDrain
}

object article {
  trait Pipeline {
    def execute[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A]
  }

  object Pipeline {
    def make: ZManaged[Any, Nothing, Pipeline] =
      for {
        queue <- Queue.unbounded[UIO[Unit]].toManaged_
        _ <-
          ZStream
            .fromQueue(queue)
            .mapMPar(2)(effect => effect)
            .runDrain
            .forkManaged
      } yield new Pipeline {
        override def execute[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
          for {
            promise <- Promise.make[E, A]
            env <- ZIO.environment[R]
            effectToPromise = effect.to(promise).unit
            effectWithoutEnv = effectToPromise.provide(env)
            interrupted <- Promise.make[Nothing, Unit]
            result <- (queue.offer(
                interrupted.await race effectWithoutEnv
              ) *> promise.await)
              .onInterrupt(interrupted.succeed(()))
          } yield result
      }
  }

  import zio.duration._

  Pipeline.make.use { pipeline =>
    pipeline
      .execute {
        ZIO.sleep(30.seconds) *> putStrLn(
          "I was processed through the pipeline"
        )
      }
      .timeout(20.seconds)
  }
}
