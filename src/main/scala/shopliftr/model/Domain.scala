package shopliftr

import java.time.LocalDateTime
import zio._
import java.time._

package object model {

  object OfferStealer {
    case class DbMongoConfig(url: String, port: String, databaseName: String)

    case class Store(
        id: String,
        name: String,
        address: String,
        city: String,
        zipcode: String,
        adZone: String
    )

    case class Promotion(
        promotionId: String,
        upc: String,
        name: String,
        saleStart: LocalDateTime,
        saleEnd: LocalDateTime
    )

    case class ShopliftrPromotion(
        id: String,
        promotion: Promotion,
        request: ShopliftrRequest,
        zipCodes: Set[String]
    )

    type RetailerId = String;
    type ChainId = String;

    case class ShopliftrRequest(
        countryCode: String,
        retailerId: RetailerId,
        chainId: ChainId
    )
  }

  object Cardlink {

    sealed trait CountryCode
    case object USA extends CountryCode
    case object Other extends CountryCode

    sealed trait IdType { self =>
      val name: String = self match {
        case Cardlink.AAID   => "aaid"
        case Cardlink.IDFA   => "idfa"
        case Cardlink.COOKIE => "coockie"
        case Cardlink.FSC    => "fsc"
      }
    }

    object IdType {
      def fromType(stringType: String): IdType =
        stringType match {
          case "aaid"    => Cardlink.AAID
          case "idfa"    => Cardlink.IDFA
          case "coockie" => Cardlink.COOKIE
          case _         => Cardlink.FSC
        }
    }

    case object AAID extends IdType
    case object FSC extends IdType
    case object IDFA extends IdType
    case object COOKIE extends IdType

    //type Network = String refined ???

    final case class CardlinkRow(
        cid: String,
        digitalId: String,
        idType: IdType
    ) {
      override def toString() = {
        s"cid: $cid digital: $digitalId, idType: $idType"
      }
    }

    final case class Identifier(
        country: CountryCode,
        network: String,
        specificId: String
    )
    object Identifier {
      //TODO error handling
      def decomposeToTuple(
          identifier: String
      ): (CountryCode, String, String) = {
        val splitted = identifier.split("-")
        val countryCode = splitted(0) match {
          case a: String if a.equals("USA") => Cardlink.USA
          case _                            => Cardlink.Other
        }

        (countryCode, splitted(1), splitted(2))
      }

      def decomposeToIdentifier(identifier: String): Identifier = {
        val t = decomposeToTuple(identifier)

        Identifier(t._1, t._2, t._3)
      }
    }

    final case class IdDocument(
        identifier: Identifier,
        idType: IdType,
        validFrom: LocalDateTime,
        relationships: Set[Relationship]
    )

    case class Relationship(identifier: Identifier, imported: LocalDateTime)

    //TODO functionnal design here
    type ElasticRequest = String

    final case class CardlinkImporter(importer: CardlinkRow => ElasticRequest) {
      self =>
      ???
    }
    object CardlinkImporter {}
  }
}
