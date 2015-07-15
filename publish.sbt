licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/vast-engineering"))

startYear := Some(2013)

pomExtra := {
	<developers>
	  <developer>
	    <id>david.pratt@gmail.com</id>
	    <name>David Pratt</name>
	    <url>https://github.com/dpratt</url>
	  </developer>
	</developers>
}

scmInfo := Some(
  ScmInfo(
    url("https://github.com/dpratt/athena"),
    "scm:git:github.com/dpratt/athena",
    Some("scm:git:git@github.com:dpratt/athena")
  )
)

bintrayReleaseOnPublish in ThisBuild := false

bintrayPackageLabels := Seq("cassandra", "driver", "scala", "akka")