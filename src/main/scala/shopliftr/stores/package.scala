package shopliftr

import java.io.IOException

import shopliftr.model.Store
import zio.{Has, IO, ZIO, ZLayer}

import scala.io.Source

package object stores {

  type Stores = Has[Stores.Service]

  object Stores {

    trait Service {
      def loadStores(path: String): IO[IOException, List[Store]]
    }

    object Service {
      val live: Service = (path: String) =>
        ZIO.effect(Source.fromResource(path)).bracket(bs => ZIO.effect(bs.close()).fold(_ => (), _ => ())) { bs =>
          ZIO.effect(bs.getLines.toList)
            .map(lines => lines
              .map(_.split(",").map(_.trim))
              .map(line => Store(line(0), line(1), line(2), line(3), line(4), line(5))))
        }.refineOrDie { case e: IOException => throw e }
    }

    val live: ZLayer[Any, IOException, Stores] = ZLayer.succeed(Service.live)

    def loadStores(path: String): ZIO[Stores, IOException, List[Store]] = ZIO.accessM(_.get.loadStores(path))
  }

}
