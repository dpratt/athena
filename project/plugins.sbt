libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % "1.8.1",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.0.4" //for setting up the test instance
)

addMavenResolverPlugin

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")