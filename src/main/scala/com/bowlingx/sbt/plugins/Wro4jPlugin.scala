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
import ro.isdc.wro.manager.factory.standalone.StandaloneContext
import ro.isdc.wro.extensions.manager.standalone.ExtensionsStandaloneManagerFactory
import ro.isdc.wro.config._
import org.mockito.Mockito
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ro.isdc.wro.http.support.DelegatingServletOutputStream
import java.io.{InputStream, FileInputStream, ByteArrayOutputStream}
import javax.servlet.FilterConfig
import ro.isdc.wro.model.resource.processor.factory.ConfigurableProcessorsFactory
import ro.isdc.wro.model.resource.ResourceType
import java.util.Properties
import ro.isdc.wro.util.provider.ConfigurableProviderSupport
import ro.isdc.wro.model.WroModelInspector
import ro.isdc.wro.model.resource.locator.factory.SimpleUriLocatorFactory
import ro.isdc.wro.extensions.locator.WebjarUriLocator
import ro.isdc.wro.model.resource.locator.StandaloneServletContextUriLocator
import org.webjars.WebJarAssetLocator
import java.util.regex.Pattern
import java.net.URLClassLoader
import ro.isdc.wro.model.resource.locator.ClasspathUriLocator
import ro.isdc.wro.model.resource.locator.ClasspathUriLocator2

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

  case class Group(name: String, lastChanged: java.util.Date)

  import scala.collection._

  val changedGroupsTracking: mutable.Map[String, Group] = mutable.Map.empty[String, Group]

  import wro4j._

  object Wro4jKeys {
    // Tasks
    lazy val generateResources = TaskKey[Set[File]]("wro4j", "Starts compiling all your defined Groups in wro.xml")
    // Settings
    lazy val targetFolder = SettingKey[File]("wro4j-target")
    lazy val wroFile = SettingKey[File]("wro4j-file", "wro.xml File")
    lazy val outputFolder = SettingKey[String]("wro4j-output-folder", "Where are all those groups written? Relative to contextFolder")
    lazy val contextFolder = SettingKey[File]("wro4j-context-folder", "Context Folder (your static resources root Dir)")
    lazy val propertiesFile = SettingKey[File]("wro4j-properties", "wro.properties File")
    lazy val processorProvider = SettingKey[ConfigurableProviderSupport]("wro4j-processor-provider", "A class that provides Processors, should extend " +
      "ro.isdc.wro.util.provider.ConfigurableProviderSupport, uses com.bowlingx.sbt.plugins.wro4j.Processors as default")
    lazy val cacheFolder = SettingKey[File]("wro4j-cache-folder", "Folder where cached calculated files are stored")
  }

  import Wro4jKeys._

  case class Manager(contextFolder: File, wroFile: File, propertiesFile: File,
                     provider: ConfigurableProviderSupport, log: Logger, cp: ClassLoader)

  private[this] def managerFactory(m: Manager) = {
    val context = new StandaloneContext()
    context.setIgnoreMissingResourcesAsString("true")
    context.setContextFoldersAsCSV(m.contextFolder.getAbsolutePath)
    context.setWroFile(m.wroFile)
    context.setMinimize(true)

    val managerFactory = new ExtensionsStandaloneManagerFactory()
    val configurable = new ConfigurableProcessorsFactory()
    val props = new Properties()
    props.load(new FileInputStream(m.propertiesFile))
    configurable.setProperties(props)

    configurable.setPostProcessorsMap(m.provider.providePostProcessors())
    configurable.setPreProcessorsMap(m.provider.providePreProcessors())

    managerFactory.initialize(context)

    managerFactory.setProcessorsFactory(configurable)

    val uriLocator = new SimpleUriLocatorFactory()
    uriLocator.addLocator(new WebjarUriLocator() {
      override def locate(uri: String): InputStream = {
        val locator = new WebJarAssetLocator(WebJarAssetLocator.getFullPathIndex(Pattern.compile(".*"), m.cp))
        m.cp.getResourceAsStream(locator.getFullPath(uri.replace(WebjarUriLocator.PREFIX, "")))
      }
    })
      .addLocator(new StandaloneServletContextUriLocator(context))
      .addLocator(new ClasspathUriLocator2() {
        override def locate(uri: String): InputStream = {
          m.log.info("locating class path resource " + uri);
          super.locate2(uri, m.cp);
          //val locator = new ClasspathA(WebJarAssetLocator.getFullPathIndex(Pattern.compile(".*"), m.cp))
        //m.cp.getResourceAsStream(locator.getFullPath(uri.replace(WebjarUriLocator.PREFIX, "")))
      }
       
      
      })

    managerFactory.setUriLocatorFactory(uriLocator)

    managerFactory.create()
  }

  def wro4jStartTask =
    (streams, sourceDirectory in generateResources, outputFolder in generateResources,
      wroFile in generateResources, contextFolder in generateResources, propertiesFile in generateResources,
      targetFolder in generateResources, processorProvider in generateResources, name in generateResources,
      cacheFolder in generateResources, fullClasspath in Compile) map {
      (out, sourcesDir, outputFolder, wroFile, contextFolder, propertiesFile, targetFolder, processorProvider,
       projectName, cache, rt) =>
        out.log.info("wro4j: == Generating Web-Resources for %s ==" format projectName)

        // Update context Classpath
        Thread.currentThread().setContextClassLoader(classOf[ro.isdc.wro.config.ReadOnlyContext].getClassLoader)

        Context.set(Context.standaloneContext())

        // External Dependencies
        val urls = rt.map(f => f.data.toURI.toURL).toArray

        //rt.map { x => out.log("*** Url: " + x) }
        
        out.log.info("wro4j-context: %s" format contextFolder.getAbsolutePath)
        out.log.info("wro4j-xml-file: %s" format wroFile.getAbsolutePath)
        out.log.info("wro4j-properties-file: %s" format propertiesFile.getAbsolutePath)

        if (contextFolder.exists() && wroFile.exists() && propertiesFile.exists()) {
          import scala.collection.JavaConversions._
          val factory = managerFactory(Manager(contextFolder, wroFile, propertiesFile, processorProvider, out.log,
            new URLClassLoader(urls)))

          val inspector = new WroModelInspector(factory.getModelFactory.create())
          val allResources = inspector.getAllUniqueResources

          allResources.map { 
                    r => out.log.info("all web resources: " + r.getUri); 
               }
          
          val cachedCompile = FileFunction.cached(cache)(inStyle = FilesInfo.lastModified,
            outStyle = FilesInfo.exists) {
            (in: ChangeReport[File], outFiles: ChangeReport[File]) =>

              
              in.modified.map { 
                    r => out.log.info("Modified web resource: " + r.getAbsolutePath); 
                    out.log.info("Context path " + contextFolder.getAbsolutePath); 
               }
              
              // Find groups that did change
              val groupNames = in.modified.flatMap(r => {
                // We need to replace the context path with found file path before wro4j work's with relative path
                
                  
                if (r.getAbsolutePath.startsWith(contextFolder.getAbsolutePath)) 
                     inspector.getGroupNamesContainingResource(r.getAbsolutePath.replace(contextFolder.getAbsolutePath, "")).toSet
                else  {
                    val g = r.getAbsolutePath.replace( (targetFolder / outputFolder).getAbsolutePath, "")
                    .replace(java.io.File.separator, "").replace(".css", "").replace(".js", "");
                    out.log.info("group: " + g)
                    Set(inspector.getGroupByName(g).getName)
                }
              })
              
             // groupNames.map { x => out.log.info("Helo There" + x); }
              
              // Find Groups that contain webjar resources, we are not able to watch for changes at the moment
              val groups = inspector.getGroupNames
              val webjarGroups = groups.map(g => inspector.getGroupByName(g)).map(g => g.getName -> g.getResources.filter(
                _.getUri.startsWith(WebjarUriLocator.PREFIX))).toMap.filter(_._2.length > 0).keySet

              if(webjarGroups.size > 0) {
                out.log.info(s"Found ${webjarGroups.size} WebJar Groups, its not possible to detect code change here...")
              }

              // Generate resources for Groups:
              (for {
                suffix <- ResourceType.values()
                groupName <- groupNames ++ webjarGroups
                relative = outputFolder
                outFile = "%s.%s" format(groupName, suffix.toString.toLowerCase)
                outputFileName = "/%s/%s.%s" format(relative, groupName, suffix.toString.toLowerCase)
                stream = {
                  out.log.info("Using relative Context: /%s%s" format(relative, outFile))

                  out.log.info("Processing Group: [%s] with type [%s]" format(groupName, suffix))
                  // Mock request, return current GroupName + Suffix
                  val request = Mockito.mock(classOf[HttpServletRequest])
                  Mockito.when(request.getRequestURI).thenReturn(outputFileName)
                  // Mock Response, write everything in ByteArray instead of delivering to Browser :)
                  val response = Mockito.mock(classOf[HttpServletResponse])
                  val createdOutputStream = new ByteArrayOutputStream()
                  Mockito.when(response.getOutputStream).thenReturn(
                    new DelegatingServletOutputStream(createdOutputStream))

                  // Initilize WebContext
                  val conf = Context.get().getConfig
                  Context.set(Context.webContext(request, response, Mockito.mock(classOf[FilterConfig])), conf)

                  factory.process()

                  createdOutputStream
                }
                if stream.size > 0
              } yield {
                val t = targetFolder / outputFolder
                t mkdirs()
                val output = t / outFile
                out.log.info("Writing Group File: [%s] with type [%s] to: %s" format(groupName, suffix, output.getAbsolutePath))
                IO.write(output, stream.toByteArray)
                stream.close()
                // Return Generated Files (for further processing)
                output
              }).toSet
          }

          // All potential changed files:
          cachedCompile(allResources.map {
            r =>{
              
              out.log.info("resource " + r.getType + " - " + r.getUri)
              if (!r.getUri.startsWith("/classpath:")) {
                  val wroResource = contextFolder / r.getUri
                  wroResource
                }
              else {
            	  var outputFileName = "";
                inspector.getGroupNamesContainingResource(r.getUri).map { 
                  g => {
                      val group = inspector.getGroupByName(g);
                      val suffix = r.getType;
                      val groupName = g
                      val relative = outputFolder
                      val outFile = "%s.%s" format(groupName, suffix.toString.toLowerCase)
                      outputFileName = "/%s/%s.%s" format(relative, groupName, suffix.toString.toLowerCase)                    
                      
                      out.log.info("out file: " + outputFileName)
                  }
                }
                
                val wroResource = targetFolder / outputFileName
                
                 out.log.info("out file: " + wroResource.getAbsolutePath)
                wroResource
              }
            }
          }.get.toSet)

        } else {
          out.log.info("No wro4j configuration found (missing Resource Folders), skipping resource creation")
          Set.empty[File]
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
    // Folder for cached Objects
    cacheFolder in generateResources <<= (cacheDirectory)(_ / "xsbt-wro4j"),
    // Generate Resource task is invoked if package command is invoked
    pack in Compile <<= (pack in Compile) dependsOn (generateResources in Compile)
  ))

}
