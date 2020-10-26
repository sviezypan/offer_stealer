package shopliftr

import zio._
import zio.stream._
import zio.kafka._
import zio.blocking.Blocking
import zio.kafka.consumer.Consumer._
import zio.clock.Clock
import zio.console._
import shopliftr.elastic.elastic_consumer._
import zio.kafka.consumer._, zio.kafka.serde._
import shopliftr.model._
import shopliftr.model.Cardlink.CardlinkRow
import shopliftr.model.Cardlink.IdDocument
import shopliftr.model.Cardlink._
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.StringDeserializer
import shopliftr.model.Cardlink.IdType
import io.circe.Encoder
import io.circe.generic.auto._, io.circe.syntax._

object KafkaConsumer extends zio.App {

  //TODO move specifics to pure config
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    createRecordChunkingStream("offerstealer")
      .provideLayer(
        Console.live ++ Clock.live ++ Blocking.live ++
          Consumer
            .make(
              ConsumerSettings(List("localhost:9092"))
                .withGroupId("offerstealer-group-1")
                .withOffsetRetrieval(
                  OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)
                )
            )
            .toLayer
      )
      .runDrain
      .exitCode

  //TODO commit offsets
  def createRecordChunkingStream(topic: String): ZStream[
    Clock with Blocking with Console with Consumer,
    Throwable,
    CardlinkRow
  ] = {
    Consumer
      .subscribeAnd(Subscription.topics(topic))
      .plainStream(Serde.long, Serde.byteArray)
      .map(_.record.value())
      .mapConcat(_.toIterable)
      .aggregate(ZTransducer.utf8Decode)
      .aggregate(ZTransducer.splitLines)
      .map(line => line.split(","))
      .map(line =>
        CardlinkRow(
          line(0),
          line(1),
          IdType.fromType(line(2))
        )
      )
      .take(5)
      .tap(cr => putStrLn(cr.asJson.toString()))
      .flatMap(cr => {
        //TOODD create Id documents with relations, then somehow send them in chunks to elastic search
        ???
        //ZStream.apply(IdDocument(Identifier),)
      })
  }

  implicit val ecnoder: Encoder[CardlinkRow] =
    Encoder.forProduct3("cid", "digitalId", "idType")(row =>
      (row.cid, row.digitalId, row.idType.name)
    )

  val upTo2048Records
      : ZTransducer[Any, Nothing, CardlinkRow, List[CardlinkRow]] =
    ZTransducer.foldUntil(List[CardlinkRow](), 2048)((l, cr) => cr :: l)

  val upTo32Mb: ZTransducer[Any, Nothing, CardlinkRow, List[CardlinkRow]] =
    ZTransducer
      .foldWeighted[CardlinkRow, List[CardlinkRow]](
        List[CardlinkRow]()
      )((list: List[CardlinkRow], row: CardlinkRow) => 1, 32 * 1024 * 1024)(
        (acc, el) => el :: acc
      )
      .map(_.reverse)

}
