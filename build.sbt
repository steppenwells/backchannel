organization := "com.gu"

name := "backchannel"

version := "1.0"

scalaVersion := "2.8.1"

// include web plugin settings in this project
seq(webSettings :_*)

resolvers ++= Seq(
  "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "repo.novus rels" at "http://repo.novus.com/releases/",
  "repo.novus snaps" at "http://repo.novus.com/snapshots/",
  "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases"
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "org.slf4j" % "slf4j-simple" % "1.6.1",
  "javax.servlet" % "servlet-api" % "2.5" % "provided->default",
  "commons-codec" % "commons-codec" % "1.5",
  "org.scalatra" %% "scalatra" % "2.0.0",
  "org.scalatra" %% "scalatra-scalate" % "2.0.0",
  "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT",
  "com.gu.openplatform" %% "content-api-client" % "1.11",
  "net.liftweb" %% "lift-json" % "2.3"
)

// and use this version of jetty for jetty run
libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % "7.3.1.v20110307" % "container"


port in container.Configuration := 9080

