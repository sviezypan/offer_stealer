package shopliftr

import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocumentReader, BSONDocumentWriter, Macros, document}
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import shopliftr.model.OfferStealer.{Promotion, ShopliftrPromotion, ShopliftrRequest}
import zio._

import scala.concurrent.ExecutionContext

package object db {

  type MongoSaver = Has[Service]

  trait Service {
    def savePromotion(promotion: ShopliftrPromotion): Task[WriteResult]

    def saveAllPromotions(promotions: List[ShopliftrPromotion]): Task[MultiBulkWriteResult]

    def findById(id: String): IO[Throwable, Option[ShopliftrPromotion]]

    def updatePromotion(shopliftrPromotion: ShopliftrPromotion): Task[WriteResult]
  }

  object MongoSaver {

    implicit def shopliftrPromotionWriter: BSONDocumentWriter[ShopliftrPromotion] = Macros.writer[ShopliftrPromotion]

    implicit def promotionWriter: BSONDocumentWriter[Promotion] = Macros.writer[Promotion]

    implicit def shopliftrRequestWriter: BSONDocumentWriter[ShopliftrRequest] = Macros.writer[ShopliftrRequest]

    implicit def shopliftrPromotionReader: BSONDocumentReader[ShopliftrPromotion] = Macros.reader[ShopliftrPromotion]

    implicit def promotionReader: BSONDocumentReader[Promotion] = Macros.reader[Promotion]

    implicit def shopliftrRequestReader: BSONDocumentReader[ShopliftrRequest] = Macros.reader[ShopliftrRequest]

    import ExecutionContext.Implicits.global

    object Service {
      def live(collection: BSONCollection): Service = new Service {
        override def updatePromotion(shopliftrPromotion: ShopliftrPromotion): Task[WriteResult] = {
          ZIO.fromFuture(_ => collection.update.one(document("id" -> shopliftrPromotion.promotion.promotionId), shopliftrPromotion))
        }

        override def savePromotion(shopliftrPromotion: ShopliftrPromotion): Task[WriteResult] = {
          ZIO.fromFuture(_ => collection.insert.one(shopliftrPromotion))
        }

        override def saveAllPromotions(promotions: List[ShopliftrPromotion]): Task[MultiBulkWriteResult] =
          ZIO.fromFuture(_ => collection.insert.many(promotions))

        override def findById(id: String): IO[Throwable, Option[ShopliftrPromotion]] =
          ZIO.fromFuture(_ => collection.find(document("id" -> id)).one[ShopliftrPromotion])
      }
    }

    val live: ZLayer[Has[BSONCollection], Nothing, MongoSaver] = ZLayer.fromService[BSONCollection, Service] { collection =>
      Service.live(collection)
    }

    def savePromotion(promotion: ShopliftrPromotion): ZIO[MongoSaver, Throwable, WriteResult] = ZIO.accessM(_.get.savePromotion(promotion))

    def saveAllPromotions(promotions: List[ShopliftrPromotion]): ZIO[MongoSaver, Throwable, MultiBulkWriteResult] = ZIO.accessM(_.get.saveAllPromotions(promotions))

    def findById(id:String): ZIO[MongoSaver, Throwable, Option[ShopliftrPromotion]] = ZIO.accessM(_.get.findById(id))

    def updatePromotion(shopliftrPromotion: ShopliftrPromotion): ZIO[MongoSaver, Throwable, WriteResult] = ZIO.accessM(_.get.updatePromotion(shopliftrPromotion))
  }
}
