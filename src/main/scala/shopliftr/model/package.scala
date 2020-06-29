package shopliftr

import java.time.LocalDateTime

package object model {

  case class DbMongoConfig(url: String, port: String, databaseName: String)

  case class Store(id: String, name: String, address: String, city: String, zipcode: String, adZone: String)

  case class Promotion(promotionId: String, upc: String, name: String, saleStart: LocalDateTime, saleEnd: LocalDateTime)

  case class ShopliftrPromotion(id :String, promotion: Promotion, request: ShopliftrRequest, zipCodes : Set[String])

/*  sealed trait CountryCode

  case object USA extends CountryCode

  case object EUROPE extends CountryCode

  case object JAPAN extends CountryCode*/

  type RetailerId = String;
  type ChainId = String;

  case class ShopliftrRequest(countryCode: String, retailerId: RetailerId, chainId: ChainId)

}
