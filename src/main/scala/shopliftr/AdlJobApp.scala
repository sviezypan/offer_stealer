package shopliftr

import zio._
import zio.stream._
import shopliftr.adl.AdlService._
import shopliftr.adl.AdlService.AdlStream
import shopliftr.adl.AdlService.AdlStream
import zio.console._
import shopliftr._
import shopliftr.config._
import shopliftr.adl.AdlService._
import shopliftr.adl._
import java.io.IOException


object AdlJobApp extends zio.App {

    def run(args: List[String]): URIO[ZEnv, ExitCode] = {
         adlToElasticApp.provideLayer((Configuration.live >>> shopliftr.adl.AdlService.live) ++ Console.live)
          .exitCode
    }

    val adlToElasticApp : ZIO[AdlStream with Console, IOException, Unit] = (for {
      _ <- AdlService.streamFile("digital-tech-id-map/cardlink/test_data/10_15_2020_jaro_testing/cardlink-sns.csv")
          .take(1000)
          .map(_.toChar)
          .tap(b => putStr(b.toChar.toString()))
    } yield ()).run(ZSink.drain)
}