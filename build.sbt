name := "offer_stealer"
version := "0.1"
scalaVersion := "2.12.10"

val Http4sVersion = "0.21.4"
val CirceVersion = "0.13.0"
val DoobieVersion = "0.9.0"
val ZIOVersion = "1.0.0-RC20"
val PureConfigVersion = "0.14.0"
val ReactiveMongoVersion = "0.20.11"

resolvers += "Clojars" at "http://clojars.org/repo/"


libraryDependencies ++= Seq(
  // ZIO
  "dev.zio" %% "zio" % ZIOVersion,
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC14",
  "dev.zio" %% "zio-streams" % ZIOVersion,
  "dev.zio" %% "zio-test" % ZIOVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZIOVersion % "test",
  // Http4s
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  // Circe
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceVersion,
  "io.circe" %% "circe-literal" % CirceVersion,
  // Doobie
  "org.tpolecat" %% "doobie-core" % DoobieVersion,
  "org.tpolecat" %% "doobie-h2" % DoobieVersion,
  // Mongo
    "org.reactivemongo" %% "reactivemongo" % ReactiveMongoVersion,
  //pure config
  "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
  // log4j
  "org.slf4j" % "slf4j-log4j12" % "1.7.26",
  //adl
  "com.microsoft.azure" % "azure-data-lake-store-sdk" % "2.3.8"
)

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
