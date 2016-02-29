name := """fuhsen"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  //javaJdbc,
  //cache,
  javaWs,
  "org.apache.jena" % "apache-jena-libs" % "3.0.1",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

fork in run := true

fork in run := true