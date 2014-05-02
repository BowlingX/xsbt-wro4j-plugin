xsbt-wro4j-plugin [![Build Status](https://jenkins.backchat.io/job/xsbt-wro4j-plugin/badge/icon)](https://jenkins.backchat.io/job/xsbt-wro4j-plugin/)
==========

This plugin provides a Wrapper for http://code.google.com/p/wro4j/

Inspired by wro4j-maven-plugin:
http://code.google.com/p/wro4j/source/browse/wro4j-maven-plugin/

## Requirements

* sbt 0.13 (use 0.3.0-SNAPSHOT for sbt 0.12)

## Example

In your project/plugins.sbt add:

```scala

resolvers += Resolver.url("sbt-plugin-snapshots",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("com.bowlingx" %% "xsbt-wro4j-plugin" % "0.3.3")

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
  (processorProvider in generateResources in Compile) := new MyCustomProviderForProcessors
```

The current build in provider creates the following pre and post processors (will be extended):

* CssUrlRewritingProcessor
* CssImportPreProcessor
* SemicolonAppenderPreProcessor
* LessCssProcessor
* CssDataUriPreProcessor
* CopyrightKeeperProcessorDecorator with JSMinProcessor
* YUICssCompressorProcessor
* CoffeeScriptProcessor
* SassCssProcessor
* CssDataUriPreProcessor

### Example properties file with all available processors:

```
preProcessors = cssUrlRewriting,cssImport,semicolonAppender,lessCss
postProcessors = yuiCssMin,copyrightMin
```


### Example wro.xml

```xml

<?xml version="1.0" encoding="UTF-8"?>
<groups xmlns="http://www.isdc.ro/wro"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.isdc.ro/wro wro.xsd">

    <group name="styles">
        <css>/static/styles/base.less</css>
    </group>

    <group name="scripts">
        <js>/static/js/first.js</js>
        <js>/static/js/second.js</js>
    </group>

</groups>

```

## Webjars

This Plugin supports webjars annotation:

```xml

<?xml version="1.0" encoding="UTF-8"?>
<groups xmlns="http://www.isdc.ro/wro"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.isdc.ro/wro wro.xsd">

    <group name="scripts">
        <js>webjar:dropzone.js</js>
    </group>

</groups>

```

```scala
libraryDependencies ++= Seq(
  "org.webjars" % "dropzone" % "3.7.1"
)

```

## Run compilation

This plugin depends on `package` and will then compile all your defined groups for you.
You also can compile you assets manually if you invoke `wro4j`.

All assets are written to `target/wro4j/compiled/` by default. If you use `xsbt-web-plugin` and package a `war` file,
your assets will be available in webapp/compiled/groupName.groupSuffix


## License
http://www.apache.org/licenses/LICENSE-2.0

Any contribution is welcome, just issue a pull-request or bug/feature if you found something :)