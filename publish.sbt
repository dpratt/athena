licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage in ThisBuild := Some(url("https://github.com/dpratt"))

startYear in ThisBuild := Some(2013)

pomExtra in ThisBuild := {
	<developers>
	  <developer>
	    <id>david.pratt@gmail.com</id>
	    <name>David Pratt</name>
	    <url>https://github.com/dpratt</url>
	  </developer>
	</developers>
}

scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/dpratt/athena"),
    "scm:git:github.com/dpratt/athena",
    Some("scm:git:git@github.com:dpratt/athena")
  )
)

bintrayReleaseOnPublish in ThisBuild := false

bintrayPackageLabels in ThisBuild := Seq("cassandra", "driver", "scala", "akka")