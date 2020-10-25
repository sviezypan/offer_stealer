package shopliftr.adl

import zio._
import com.microsoft.azure.datalake.store._
import com.microsoft.azure.datalake.store.ADLStoreClient
import zio.stream._
import shopliftr.config.Configuration._
import com.microsoft.azure.datalake.store.oauth2.ClientCredsTokenProvider
import java.io.IOException
import blocking._

object adlService {

  type AdlStream = Has[Service]

  trait Service {
    def streamFile(fileName: String): ZStream[Any, IOException, Byte]
  }

  object Service {

    def live(config: AzureDataLakeConfig): Service =
      (filename: String) => {
        val tokenProvider: ClientCredsTokenProvider = new ClientCredsTokenProvider(config.tokenUrl, config.clientId, config.secret)
        
        ZStream.fromEffect(ZIO.effect(ADLStoreClient.createClient(config.url, tokenProvider)))
          .map(client => client.getReadStream(filename)).refineOrDie{case e:IOException => e}
          .flatMap(is => ZStream.fromInputStream(is).provideLayer(Blocking.live))
      }
  }

  val live: ZLayer[Configuration, IOException, AdlStream] =
    ZLayer.fromService[AzureDataLakeConfig, Service] { config =>
      Service.live(config)
    }

  def streamFile(fileName: String): ZStream[AdlStream, IOException, Byte] =
    ZStream.accessStream(_.get.streamFile(fileName))
}
