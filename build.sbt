import xml.Group

organization := "com.bowlingx"

name := "xsbt-wro4j-plugin"

version := "0.3.4"

scalaVersion := "2.10.3"

sbtPlugin := true

ScriptedPlugin.scriptedSettings

scriptedBufferLog := false

net.virtualvoid.sbt.graph.Plugin.graphSettings

scalacOptions += "-unchecked"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
   "commons-logging" % "commons-logging" % "1.1.1" % "provided",
   "org.slf4j" % "log4j-over-slf4j" % "1.6.6",
   "ch.qos.logback" % "logback-classic" % "1.0.7" ,
   "org.mockito" % "mockito-core" % "1.9.5",
   "javax.servlet" % "javax.servlet-api" % "3.1.0",
   "ro.isdc.wro4j" % "wro4j-core" % "1.7.5" excludeAll(ExclusionRule(organization = "org.slf4j")),
   "ro.isdc.wro4j" % "wro4j-extensions" % "1.7.5" excludeAll(ExclusionRule(organization = "org.slf4j"))
)

publishMavenStyle := false

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
                       ("scalasbt-sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                     else
                       ("scalasbt-sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

packageOptions <<= (packageOptions, name, version, organization) map {
  (opts, title, version, vendor) =>
     opts :+ Package.ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Vendor" -> "BowlingX",
      "Specification-Version" -> version,
      "Implementation-Title" -> title,
      "Implementation-Version" -> version,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> "BowlingX",
      "Implementation-Url" -> "https://github.com/BowlingX/xsbt-wro4j-plugin"
     )
}

homepage := Some(url("https://github.com/BowlingX/xsbt-wro4j-plugin"))

startYear := Some(2012)

licenses := Seq(("Apache 2.0", url("https://github.com/BowlingX/xsbt-wro4j-plugin/raw/HEAD/LICENSE")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
  <scm>
    <connection>scm:git:git://github.com/BowlingX/xsbt-wro4j-plugin.git</connection>
    <developerConnection>scm:git:git@github.com:BowlingX/xsbt-wro4j-plugin.git</developerConnection>
    <url>https://github.com/BowlingX/xsbt-wro4j-plugin</url>
  </scm>
  <developers>
    <developer>
      <id>BowlingX</id>
      <name>David Heidrich</name>
      <url>http://www.myself-design.com/</url>
    </developer>
  </developers>
)}
