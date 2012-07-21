xsbt-wro4j-plugin
==========

This plugin provides a Wrapper for http://code.google.com/p/wro4j/

Inspired by wro4j-maven-plugin:
http://code.google.com/p/wro4j/source/browse/wro4j-maven-plugin/

## Requirements

* sbt 0.11.x

## Example

I currently don't have any repository available, so please just do publish-local for testing
I will publish it as soon as possible

```
 git pull git://github.com/BowlingX/xsbt-wro4j-plugin.git
 sbt publish-local

 // Run Tests (if you like :)):
 sbt scripted
```

In your project/plugins.sbt add:

```scala

addSbtPlugin("com.bowlingx" %% "xsbt-wro4j-plugin" % "0.1.0-SNAPSHOT")

```

In your build.sbt add:

```scala

import com.bowlingx.sbt.plugins.Wro4jPlugin._
import Wro4jKeys._

// import task settings
seq(wro4jSettings: _*)

// If you use xsbt-web-plugin, this will add compiled files to your war file:
(webappResources in Compile) <+= (targetFolder in generateResources in Compile)

```

### Prepare wro4j configuration

Wro4j is configured through wro.xml and wro.properties file in `src/main/webapp/WEB-INF`

* `wro.xml` contains group definitions for file resources (like css, less, js Files).
* `wro.properties` contains pre -and post processor configuration.

The current implementation reads those provider informations (shortcuts) from classes extending `com.bowlingx.sbt.plugins.wro4j.Processors`
You can easily provide your custom Provider if you override  `processorProvider in generateResources` e.g.

```scala
  (processorProvider in generateResources in Compile := new MyCustomProviderForProcessors)
```

The current build in provider creates the following pre and post processors (will be extended):

* CssUrlRewritingProcessor
* CssImportPreProcessor
* SemicolonAppenderPreProcessor
* LessCssProcessor
* CssDataUriPreProcessor
* CopyrightKeeperProcessorDecorator with JSMinProcessor
* YUICssCompressorProcessor

Example properties File with all available processors:

```
preProcessors = cssUrlRewriting,cssImport,semicolonAppender,lessCss
postProcessors = yuiCssMin,copyrightMin
```

## License
http://www.apache.org/licenses/LICENSE-2.0

Any contribution is welcome, just issue a pull-request or bug/feature if you found something :)