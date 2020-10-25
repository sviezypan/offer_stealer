package shopliftr.elastic

import zio._
import zio.kafka._
import zio.stream._
import zio.console._
import zio.clock._
import zio.blocking._
import zio.kafka.consumer.{CommittableRecord, OffsetBatch}

object elastic_consumer {

  //TODO tidy up type params & zlayer according to best practices
  type ElasticWriter = Has[ElasticWriter.Service]

  object ElasticWriter {
    trait Service {
      def process[R <: Has[_]](
          stream: ZStream[
            R with Console,
            Throwable,
            CommittableRecord[Long, String]
          ]
      ): ZIO[R, Throwable, OffsetBatch]
    }
  }

  def process[R <: Has[_]](
      stream: ZStream[
        R,
        Throwable,
        CommittableRecord[Long, String]
      ]
  ): ZStream[R with ElasticWriter, Throwable, OffsetBatch] =
    ZStream.accessM(_.get[ElasticWriter.Service].process[R](stream))

  def live[R <: Has[_]]: ZLayer[R with Console, Throwable, ElasticWriter] =
    ZLayer.fromFunction(console =>
      new ElasticWriter.Service {
        def process[R <: Has[_]](stream: ZStream[R with zio.console.Console, Throwable, CommittableRecord[Long,String]]): 
                ZIO[R, Throwable, OffsetBatch] = 
        stream
            .tap(data => putStrLn(data.record.value))
            .run(batchOffsets) 
            .provideSomeLayer[R](Console.live)
      }
    )

  private val batchOffsets: ZSink[Any, Throwable, CommittableRecord[Long, String], Nothing, OffsetBatch] =
    ZSink.foldLeft(OffsetBatch.empty) {
      (acc, rec: CommittableRecord[Long, String]) =>
        acc.merge(rec.offset)
    }
}
