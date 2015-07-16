organization in ThisBuild := "io.athena"

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions in ThisBuild := Seq("2.11.7", "2.10.5")

name := "athena"

description := "A fully nonblocking and asynchronous client library for Cassandra."

// Force compilation in java 1.6
javacOptions in Compile in ThisBuild ++= Seq("-source", "1.6", "-target", "1.6")

//Don't publish the root project
publishArtifact := false
publish := {}
publishLocal := {}

lazy val core = Project("athena-core", file("core"))

lazy val client = Project("athena-client", file("client")).dependsOn(core)