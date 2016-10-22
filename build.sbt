import scalariform.formatter.preferences.{ DoubleIndentClassDeclaration, FormatXml, PreserveDanglingCloseParenthesis }

name := "c360sparkle"

version := "0.1-SNAPSHOT"

maintainer := "Context360 Analytics <admin@context360.com>"

packageSummary := "Context360 Analytics Package"

packageDescription := """Context360 Analytics installation package for Linux."""

packageName in Rpm := "c360sparkle"

rpmVendor := "Context360 Inc."

rpmLicense := Some("Apache License")

rpmGroup := Some("Web Services")

rpmBrpJavaRepackJars := true

scalaVersion := "2.11.8"

val PhantomVersion = "1.22.0"

routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps")

bashScriptExtraDefines ++= Seq(
  """addJava "-Dnewrelic.config.file=${lib_dir}/../conf/newrelic.yml"""",
  """addJava "-javaagent:${lib_dir}/com.newrelic.agent.java.newrelic-agent-3.30.0.jar""""
)

updateOptions in Global := updateOptions.in(Global).value.withCachedResolution(true)

lazy val root = sbt.project.in(file(".")).enablePlugins(PlayScala, SbtNativePackager, LinuxPlugin)

// Resolver is needed only for SNAPSHOT versions
resolvers ++= Seq(
  "Typesafe repository snapshots"     at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases"      at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                     at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                 at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"                at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype staging"                  at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository"        at "http://download.java.net/maven/2/",
  "Twitter Repository"                at "http://maven.twttr.com",
  "Websudos"                          at "https://dl.bintray.com/websudos/oss-releases/",
  "Scalaz Bintray"                    at "https://dl.bintray.com/scalaz/releases",
  "Atlassian Releases"                at "https://maven.atlassian.com/public/"
)

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.websudos"            %%  "phantom-dsl"          % PhantomVersion,
  "com.mohiva"              %%  "play-silhouette"                 % "4.0.0-BETA4",
  "com.mohiva"              %%  "play-silhouette-password-bcrypt" % "4.0.0-BETA4",
  "com.mohiva"              %%  "play-silhouette-persistence"     % "4.0.0-BETA4",
  "com.mohiva"              %%  "play-silhouette-testkit"         % "4.0.0-BETA4" % "test",
  "net.codingwell"          %%  "scala-guice"          % "4.0.1",
  "net.ceedubs"             %%  "ficus"                % "1.1.2",
  "com.adrianhurt"          %%  "play-bootstrap"       % "1.0-P25-B3",
  "org.webjars"             %   "font-awesome"         % "4.5.0",
  "org.webjars"             %   "bootstrap-datepicker" % "1.4.0",
  "com.newrelic.agent.java" %   "newrelic-agent"       % "3.30.0",
  "org.logback-extensions"  %   "logback-ext-loggly"   % "0.1.2",
  "org.scalatestplus.play"  %%  "scalatestplus-play"   % "1.5.0" % "test"
)

// To speed up compilation you can disable documentation generation:

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)

val deployTask = TaskKey[Unit]("deploy", "Prepare Deploy packages")

deployTask <<= (
  packageBin in Compile,
  Keys.baseDirectory,
  Keys.name,
  Keys.version) map { (pb, bd, name, version) =>

  val sourceDeb = bd / ("target/" + name + "_" + version + "_all.deb")
  val destDeb = bd / "deploy" / (name + ".deb")
  val sourceRpm = bd / ("target/rpm/RPMS/noarch/" + name + "-" + version + "-1.noarch.rpm")
  val destRpm = bd / "deploy" / (name + ".rpm")

  println("deployTask: " + pb + "\n" + name + ", " + version)

  IO.delete(bd / "deploy" / (name + ".deb"))
  IO.delete(bd / "deploy" / (name + ".rpm"))
  if (sourceDeb.exists()) {
    IO.copyFile(sourceDeb, destDeb)
  }
  if (sourceRpm.exists()) {
    IO.copyFile(sourceRpm, destRpm)
  }
}

fork in run := true
