import sbt._

object Dependencies {

  def akka(artifact: String) = "com.typesafe.akka" %% ("akka-" + artifact) % "2.3.12"

  val akkaActor = akka("actor")
  val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"
  val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

  //test dependencies
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val akkaSlf4j = akka("slf4j")
  val akkaTestkit = akka("testkit")

}