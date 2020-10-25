package shopliftr

import zio._
import zio.stream._
import zio.kafka._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import shopliftr.elastic.elastic_consumer._
import zio.kafka.consumer._, zio.kafka.serde._

object KafkaConsumer extends zio.App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] = 
    createRecordChunkingStream("offerstealer")
      .provideLayer(Console.live ++ Clock.live ++ Blocking.live ++ 
          Consumer.make(ConsumerSettings(List("localhost:9092")).withGroupId("offerstealer-group-2")).toLayer)
      .take(1000)
      .runDrain
      .exitCode

  def createRecordChunkingStream(topic: String) : ZStream[/*Blocking with Clock with ElasticWriter with*/ Clock with Blocking with Console with Consumer, 
  Throwable, String] = {
    Consumer.subscribeAnd(Subscription.topics(topic))
      .plainStream(Serde.long, Serde.string)
      .map(_.record.value())
      .tap(value => putStrLn(value))
      //.via(s => process(s))
      //.mapM(_.commit)
  }
}
