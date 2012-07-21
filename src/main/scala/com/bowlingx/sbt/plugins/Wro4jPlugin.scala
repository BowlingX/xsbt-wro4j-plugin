/**
 * A SBT Plugin for wro4j (http://code.google.com/p/wro4j/)
 *
 * Copyright 2012 David Heidrich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bowlingx.sbt.plugins

import sbt._
import sbt.Keys.{`package` => pack, _}
import ro.isdc.wro.manager.factory.standalone.{InjectableContextAwareManagerFactory, StandaloneContext}
import ro.isdc.wro.extensions.manager.standalone.ExtensionsStandaloneManagerFactory
import ro.isdc.wro.config._
import org.mockito.Mockito
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ro.isdc.wro.http.support.DelegatingServletOutputStream
import java.io.{FileInputStream, ByteArrayOutputStream}
import javax.servlet.FilterConfig
import ro.isdc.wro.model.resource.processor.factory.ConfigurableProcessorsFactory
import ro.isdc.wro.model.resource.ResourceType
import java.util.Properties
import ro.isdc.wro.util.provider.ConfigurableProviderSupport

/**
 * A wro4j SBT Plugin
 *
 * {{{
 *   // In your build.sbt do:
 *
 *   import com.bowlingx.sbt.plugins.Wro4jPlugin._
 *
 *   seq(wro4jSettings:_*)
 *
 * }}}
 *
 */
object Wro4jPlugin extends Plugin {

  import wro4j._

  object Wro4jKeys {
    // Tasks
    lazy val generateResources = TaskKey[Seq[File]]("wro4j", "Starts compiling all your definied Groups in wro.xml")
    // Settings
    lazy val targetFolder = SettingKey[File]("wro4j-target")
    lazy val wroFile = SettingKey[File]("wro4j-file", "wro.xml File")
    lazy val outputFolder = SettingKey[String]("wro4j-output-folder", "Where are all those groups written? Relative to contextFolder")
    lazy val contextFolder = SettingKey[File]("wro4j-context-folder", "Context Folder (your static resources root Dir)")
    lazy val propertiesFile = SettingKey[File]("wro4j-properties", "wro.properties File")
    lazy val processorProvider = SettingKey[ConfigurableProviderSupport]("wro4j-processor-provider", "A class that provides Processors, should extend " +
      "ro.isdc.wro.util.provider.ConfigurableProviderSupport, uses com.bowlingx.sbt.plugins.wro4j.Processors as default")
  }

  import Wro4jKeys._

  case class Manager(contextFolder: File, wroFile: File, propertiesFile: File, provider: ConfigurableProviderSupport)

  private[this] def managerFactory(m: Manager) = {
    val context = new StandaloneContext()
    context.setIgnoreMissingResources(true)
    context.setContextFolder(m.contextFolder)
    context.setWroFile(m.wroFile)
    context.setMinimize(true)

    val managerFactory = new InjectableContextAwareManagerFactory(
      new ExtensionsStandaloneManagerFactory())
    val configurable = new ConfigurableProcessorsFactory()
    val props = new Properties()
    props.load(new FileInputStream(m.propertiesFile))
    configurable.setProperties(props)

    configurable.setPostProcessorsMap(m.provider.providePostProcessors())
    configurable.setPreProcessorsMap(m.provider.providePreProcessors())

    managerFactory.initialize(context)

    managerFactory.setProcessorsFactory(configurable)
    managerFactory.create()
  }

  def wro4jStartTask =
    (streams, sourceDirectory in generateResources, outputFolder in generateResources,
      wroFile in generateResources, contextFolder in generateResources, propertiesFile in generateResources,
      targetFolder in generateResources, processorProvider in generateResources) map {
      (out, sourcesDir, outputFolder, wroFile, contextFolder, propertiesFile, targetFolder, processorProvider) =>
        out.log.info("wro4j: == Generating Web-Resources ==")

        Context.set(Context.standaloneContext())

        out.log.info("wro4j-context: %s" format contextFolder.getAbsolutePath)
        out.log.info("wro4j-xml-file: %s" format wroFile.getAbsolutePath)
        out.log.info("wro4j-properties-file: %s" format propertiesFile.getAbsolutePath)

        if (contextFolder.exists() && wroFile.exists() && propertiesFile.exists()) {

          import scala.collection.JavaConversions._
          val factory = managerFactory(Manager(contextFolder, wroFile, propertiesFile, processorProvider))

          (for {
            suffix <- ResourceType.values()
            groupName <- factory.getModelFactory.create().getGroupNames

            val relative = outputFolder
            val outFile = "%s.%s" format(groupName, suffix.toString.toLowerCase)
            val outputFileName = "/%s/%s.%s" format(relative, groupName, suffix.toString.toLowerCase)
            val stream = {
              out.log.info("Using relative Context: /%s%s" format(relative, outFile))

              out.log.info("Processing Group: [%s] with type [%s]" format(groupName, suffix))
              // Mock request, return current GroupName + Suffix
              val request = Mockito.mock(classOf[HttpServletRequest])
              Mockito.when(request.getRequestURI).thenReturn(outputFileName)
              // Mock Response, write everything in ByteArray instead of delivering to Browser :)
              val response = Mockito.mock(classOf[HttpServletResponse])
              val createdOutputStream = new ByteArrayOutputStream()
              Mockito.when(response.getOutputStream).thenReturn(new DelegatingServletOutputStream(createdOutputStream))

              // Initilize WebContext
              val conf = Context.get().getConfig
              Context.set(Context.webContext(request, response, Mockito.mock(classOf[FilterConfig])), conf)

              factory.process()

              createdOutputStream.toByteArray
            }
            if stream.length > 0
          } yield {
            val t = targetFolder / outputFolder
            t mkdirs()
            val output = t / outFile
            out.log.info("Writing Group File: [%s] with type [%s] to: %s" format(groupName, suffix, output.getAbsolutePath))
            IO.write(output, stream)
            // Return Generated Files (for further processing)
            output
          }).toSeq
        } else {
          out.log.info("No wro4j configuration found (missing Resource Folders), skipping resource creation")
          Seq.empty[File]
        }
    }

  lazy val wro4jSettings = inConfig(Compile)(Seq(
    // Default ContextFolder
    contextFolder in generateResources <<= (sourceDirectory in Compile)(_ / "webapp"),
    // Target folder for compiled resources
    targetFolder in generateResources <<= (target in Compile)(_ / "wro4j"),
    // Default WroFile
    wroFile in generateResources <<= (contextFolder in generateResources)(_ / "WEB-INF" / "wro.xml"),
    // Default output Folder (relative Path to contextFolder)
    outputFolder in generateResources := "compiled/",
    // wro4j Properties file
    propertiesFile in generateResources <<= (contextFolder in generateResources)(_ / "WEB-INF" / "wro.properties"),
    // Processor Provider (provides wro4j processors to use in wro.properties)
    processorProvider in generateResources := new Processors,
    // Generate Task
    generateResources <<= wro4jStartTask,
    // Generate Resource task is invoked if package command is invoked
    pack in Compile <<= (pack in Compile) dependsOn (generateResources in Compile)
  ))


}
