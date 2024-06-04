lazy val http4sVersion = "0.23.16"
lazy val blazeVersion = "0.23.16"
lazy val circeVersion = "0.14.6"
lazy val catsEffectVersion = "3.3.11"
lazy val sttpVersion = "3.8.3"

lazy val root = (project in file("."))
  .settings(
    name := "keycloak4s-http4s-example",
    version := "0.1.0",
    scalaVersion := "2.13.13",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.7.0",
      "io.monix" %% "monix" % "3.4.1",
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "is.cir" %% "ciris" % "2.3.2",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres-circe" % "1.0.0-RC1",
      "com.github.fd4s" %% "fs2-kafka" % "3.5.1",
      "org.http4s" %% "http4s-blaze-client" % blazeVersion,
      "org.http4s" %% "http4s-blaze-server" % blazeVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.typelevel" %% "log4cats-slf4j" % "2.3.0",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "dev.hnaderi" %% "scala-k8s-client" % "0.17.0",
      "dev.hnaderi" %% "scala-k8s-http4s-blaze" % "0.17.0",
      "dev.hnaderi" %% "scala-k8s-circe" % "0.17.0",
      "com.fullfacing" %% "keycloak4s-core-ce3" % "3.2.1",
      "com.fullfacing" %% "keycloak4s-admin-ce3" % "3.2.1",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-monix" % sttpVersion,
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % "3.2.2.0" % Test
    ),
    // Configurar el esquema de versiones para evitar conflictos
    ThisBuild / libraryDependencySchemes ++= Seq(
      "io.circe" %% "circe-core" % VersionScheme.EarlySemVer,
      "io.circe" %% "circe-generic" % VersionScheme.EarlySemVer,
      "io.circe" %% "circe-parser" % VersionScheme.EarlySemVer,
      "org.typelevel" %% "cats-effect" % VersionScheme.EarlySemVer
    ),
    // Forzar versiones específicas para evitar conflictos
    dependencyOverrides ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.11",
      "io.monix" %% "monix-catnap" % "3.4.1"
    )
  )
