addSbtPlugin("com.bowlingx" %% "xsbt-wro4j-plugin" % "0.1.0")

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11.1"))
