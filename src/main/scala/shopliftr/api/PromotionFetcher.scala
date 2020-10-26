package shopliftr

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import fs2.Stream
import io.circe.{Decoder, HCursor}
import org.http4s._
import org.http4s.client.Client
import shopliftr.model.OfferStealer.Promotion
import zio._

import scala.util.{Failure, Success, Try}

package object api {

  object ShopliftrApi {

    type ShopliftrApi = Has[Service]

    trait Service {
      def getPromotions(zipCode: String): Task[List[Promotion]]
    }

    object Service {

      val shopliftrUrl = "https://api.shopliftr.com/v3/deals/search"

      implicit val dateDecoder: Decoder[LocalDateTime] = Decoder.decodeString.emap[LocalDateTime](str => {
        Try(LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)) match {
          case Success(value) => Right(value)
          case Failure(exception) => Left(exception.getLocalizedMessage)
        }
      })


      implicit val decodePromotion: Decoder[Promotion] = (c: HCursor) => for {
        id <- c.downField("id").as[String]
        upc <- c.downField("upc").as[String]
        name <- c.downField("name").as[String]
        saleStart <- c.downField("sale_start").as[LocalDateTime]
        saleEnd <- c.downField("sale_end").as[LocalDateTime]
      } yield {
        Promotion(id, upc, name, saleStart, saleEnd)
      }

      implicit val listPromotionDecoder: Decoder[List[Promotion]] = (c: HCursor) =>
        c.downField("groups").values.get.toList
          .flatMap(_.findAllByKey("results"))
          .flatMap(json => json.asArray.get.toList)
          .map(_.as[Promotion]).partition(_.isLeft) match {
          case (Nil, decoders) => Right(decoders.map(_.right.get))
          case (fails, Nil) => Left(fails.map(_.left.get).head)
          case (_, decoders) => Right(decoders.map(_.right.get))
        }

      private def createBody(zipCode: String): Seq[Byte] =
        ("{\"filter\": {\"location\": {\"zip\": \"" + zipCode + "\"}, \"chain\":{\"id\":[\"78\"]}}, \"sort\": [ {\"brand\": \"asc\"}, \"default\"], \"from\": 0, \"size\": 1000}").getBytes

      import org.http4s.circe.CirceEntityDecoder._
      import zio.interop.catz._

      def live(http4sClient: Client[Task]): Service = (zipCode: String) =>
        http4sClient.expect[List[Promotion]](new Request[Task](Method.POST,
          Uri.unsafeFromString(shopliftrUrl),
          HttpVersion.`HTTP/1.1`,
          Headers.of(Header.apply("x-api-key", "TODO"), Header.apply("Content-Type", "application/json")),
          Stream.emits[Task, Byte](createBody(zipCode))))
    }

    val live: ZLayer[Has[Client[Task]], Throwable, ShopliftrApi] = ZLayer.fromService[Client[Task], Service] { http4sClient =>
      Service.live(http4sClient)
    }

    def getPromotions(zipCode: String): ZIO[ShopliftrApi, Throwable, List[Promotion]] = ZIO.accessM(_.get.getPromotions(zipCode))
  }

}
