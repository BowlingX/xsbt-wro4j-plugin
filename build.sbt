organization := "com.bowlingx"

name := "xsbt-wro4j-plugin"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

sbtPlugin := true

ScriptedPlugin.scriptedSettings

libraryDependencies ++= Seq(
   "commons-logging" % "commons-logging" % "1.1.1" % "provided",
   "org.slf4j" % "log4j-over-slf4j" % "1.6.4",
   "ch.qos.logback" % "logback-classic" % "1.0.2",
   "org.specs2" %% "specs2" % "1.11" % "test",
   "org.mockito" % "mockito-core" % "1.9.0",
   "javax.servlet" % "javax.servlet-api" % "3.0.1",
   "ro.isdc.wro4j" % "wro4j-core" % "1.4.7" excludeAll(ExclusionRule(organization = "org.slf4j")),
   "ro.isdc.wro4j" % "wro4j-extensions" % "1.4.7" excludeAll(ExclusionRule(organization = "org.slf4j"))
)
