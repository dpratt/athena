import Dependencies._

name := "athena-core"

def play(artifact: String) = "com.typesafe.play" %% ("play-" + artifact) % "2.3.9"

libraryDependencies ++= Seq(
  akkaActor,
  play("json"),
  play("iteratees"),
  scalaTest % "test",
  logback % "test",
  akkaSlf4j % "test",
  akkaTestkit % "test"
)

CassandraUtils.cassandraTestSettings
