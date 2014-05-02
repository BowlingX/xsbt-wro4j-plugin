import com.bowlingx.sbt.plugins.Wro4jPlugin._
import Wro4jKeys._

version := "0.1"

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-websocket" % "8.1.4.v20120524",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.4.v20120524" % "container",
  "org.webjars" % "dropzone" % "3.7.1"
)


seq(webSettings :_*)

seq(wro4jSettings: _*)

(webappResources in Compile) <+= (targetFolder in generateResources in Compile)

ivyXML :=
  <dependencies>
    <exclude org="org.eclipse.jetty.orbit" />
  </dependencies>

