package shopliftr.config

import zio._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Configuration {

  final case class OfferStealerApiConfig(url: String, xApiKey: String)
  final case class AzureDataLakeConfig(tokenUrl :String, clientId: String, secret: String, url: String)
  final case class AppConfig(offerStealer: OfferStealerApiConfig, adl: AzureDataLakeConfig)

  val live: ULayer[Configuration] = ZLayer.fromEffectMany(
    ZIO
      .effect(ConfigSource.default.loadOrThrow[AppConfig])
      .map(c => Has(c.offerStealer) ++ Has(c.adl))
      .orDie
  )

   type Configuration = Has[AzureDataLakeConfig] with Has[OfferStealerApiConfig]
}