package shopliftr

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import cats.effect.{ContextShift, IO, Timer}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, DefaultDB, MongoConnection}
import shopliftr.api.ShopliftrApi
import shopliftr.api.ShopliftrApi.ShopliftrApi
import shopliftr.db.MongoSaver
import shopliftr.model.{DbMongoConfig, ShopliftrPromotion, ShopliftrRequest}
import shopliftr.stores.Stores
import zio.clock.Clock
import zio.console._
import zio.duration.Duration
import zio.interop.catz
import zio.{ExitCode, Task, _}

import scala.concurrent.{ExecutionContext, Future}
import shopliftr.config.Configuration
import shopliftr.adl.adlService.AdlStream

object OfferStealerApp extends App {

  val shopliftrApiLayer: ZLayer[Any, Throwable, ShopliftrApi] = clientManaged.toLayer >>> ShopliftrApi.live

  val mongoSaverLayer: ZLayer[Any, Throwable, MongoSaver] = shopliftrCollection >>> MongoSaver.live

  val fullLayer: ZLayer[Any, Throwable, Stores with ShopliftrApi with MongoSaver] = Stores.live ++ shopliftrApiLayer ++ mongoSaverLayer

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    shopliftrApp.provideLayer(Console.live ++ Clock.live ++ fullLayer).fold(_ => ExitCode.success, _ => ExitCode.failure)

  val shopliftrApp: ZIO[Clock with Console with Stores with ShopliftrApi with MongoSaver, Throwable, Unit] =
    for {
      _ <- Clock.Service.live.currentDateTime.flatMap(time => putStrLn(time.format(DateTimeFormatter.ISO_DATE_TIME)))
      zipCodes <- Stores.loadStores("stores.csv").map(_.map(_.zipcode).distinct)
      promotions <- ZIO.foreachPar(zipCodes)(zipCode => ShopliftrApi.getPromotions(zipCode)
        .map(_.map(promotion => ShopliftrPromotion(promotion.promotionId, promotion, ShopliftrRequest("USA", "8", "78"), Set(zipCode)))))
        .map(_.flatten)

      flatten <- ZIO.succeed(reduce(List(), promotions))


      _ <- MongoSaver.saveAllPromotions(flatten)
      _ <- putStrLn("LETS CHECK END: ") *> Clock.Service.live.currentDateTime.flatMap(time => putStrLn(time.format(DateTimeFormatter.ISO_DATE_TIME))) *> Clock.Service.live.sleep(Duration(10, TimeUnit.SECONDS))
    } yield ()

  def reduce(accumulator: List[ShopliftrPromotion], original: List[ShopliftrPromotion]): List[ShopliftrPromotion] = original match {
    case Nil => accumulator
    case head :: xs => {
      val newAcc = accumulator ++ List(xs.filter(_.id.equals(head.id)).foldLeft(head)((a, b) => a.copy(zipCodes = a.zipCodes ++ b.zipCodes)))
      reduce(newAcc, xs.filter(!_.id.equals(head.id)))
    }
  }

  import scala.concurrent.ExecutionContext.global

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  import org.http4s.client._
  import org.http4s.client.blaze._
  import zio.interop.catz._

  def clientManaged: ZManaged[Any, Throwable, Client[Task]] = {
    val zioManaged: ZIO[Any, Throwable, ZManaged[Any, Throwable, Client[Task]]] = ZIO.runtime[Any].map { rts =>
      implicit def rr = rts

      catz.catsIOResourceSyntax(BlazeClientBuilder[Task](ExecutionContext.global).resource).toManaged
    }
    val zm = zioManaged.toManaged_
    zm.flatten
  }

  def shopliftrCollection: ZLayer[Any, Throwable, Has[BSONCollection]] = ZLayer.fromEffect {
    DbMongoConfig("localhost", "27017", "shopliftr")
    val mongoUri = "mongodb://localhost:27017/"

    import ExecutionContext.Implicits.global

    val driver: AsyncDriver = AsyncDriver()
    val parsedUri: Future[MongoConnection.ParsedURI] = MongoConnection fromString mongoUri
    val futureConnection: Future[MongoConnection] = parsedUri.flatMap(driver.connect)

    def shopliftrDb: Task[DefaultDB] = ZIO.fromFuture(_ => futureConnection.flatMap(_.database("shopliftr")))

    shopliftrDb.map(_.collection("shopliftrPromotionV2"))
  }
}
