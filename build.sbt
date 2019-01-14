name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.19"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.google.inject" % "guice" % "4.1.0",
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.5",
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-spray-json" % "1.0.1",
  "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.1",
  "org.sangria-graphql" %% "sangria-monix" % "1.0.0"
)
