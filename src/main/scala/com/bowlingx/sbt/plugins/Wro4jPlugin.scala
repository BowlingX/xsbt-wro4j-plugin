package com.bowlingx.sbt.plugins

import sbt._
import Keys._
import ro.isdc.wro.manager.factory.standalone.{InjectableContextAwareManagerFactory, StandaloneContext}
import ro.isdc.wro.extensions.manager.standalone.ExtensionsStandaloneManagerFactory
import ro.isdc.wro.config._
import org.mockito.Mockito
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ro.isdc.wro.http.support.DelegatingServletOutputStream
import java.io.{ByteArrayOutputStream}
import javax.servlet.FilterConfig
import ro.isdc.wro.model.resource.processor.factory.{ConfigurableProcessorsFactory, DefaultProcesorsFactory}
import ro.isdc.wro.model.resource.ResourceType
import ro.isdc.wro.extensions.processor.css.LessCssProcessor
import java.util.Properties

/**
 * A Wro4j Plugin
 */

object Wro4jPlugin extends Plugin {

  object Wro4jKeys {
    // Tasks
    val generateResources = TaskKey[Array[File]]("wro4j")
    // Settings
    val wroFile = SettingKey[File]("wro-file")
    val outputFolder = SettingKey[File]("output-folder")
    val contextFolder = SettingKey[File]("context-folder")
  }

  import Wro4jKeys._

  private[this] def managerFactory(contextFolder:File, wroFile:File) = {
    val context = new StandaloneContext()
    context.setIgnoreMissingResources(true)
    context.setContextFolder(contextFolder)
    context.setWroFile(wroFile)
    context.setMinimize(true)

    val managerFactory = new InjectableContextAwareManagerFactory(
      new ExtensionsStandaloneManagerFactory())

    managerFactory.initialize(context)
    val processor = new DefaultProcesorsFactory()
    processor.addPostProcessor(new LessCssProcessor())
    managerFactory.setProcessorsFactory(processor)
    managerFactory.create()
  }

  private def lessCompilerTask =
    (streams, sourceDirectory in generateResources, outputFolder in generateResources,
      wroFile in generateResources, contextFolder in generateResources) map {
      (out, sourcesDir, outputFolder, wroFile, contextFolder) =>
        out.log.info("Generating Web-Resources")

        Context.set(Context.standaloneContext())

        import scala.collection.JavaConversions._
        val factory = managerFactory(contextFolder, wroFile)
        for {
          suffix <- ResourceType.values()
          groupName <- factory.getModelFactory.create().getGroupNames
        } yield {
          out.log.info("Processing Group: [%s] with type [%s]" format (groupName, suffix))
          // Mock request, return current GroupName + Suffix
          val outputFileName = "%s.%s" format (groupName, suffix)
          val request = Mockito.mock(classOf[HttpServletRequest])
          Mockito.when(request.getRequestURI).thenReturn(outputFileName)
          // Mock Response, write everything in ByteArray instead of delivering to Browser :)
          val response = Mockito.mock(classOf[HttpServletResponse])
          val createdOutputStream = new ByteArrayOutputStream()
          Mockito.when(response.getOutputStream).thenReturn(new DelegatingServletOutputStream(createdOutputStream))

          //init context
          val conf = Context.get().getConfig
          Context.set(Context.webContext(request, response, Mockito.mock(classOf[FilterConfig])), conf)

          factory.process()

          outputFolder.mkdirs()
          IO.write(outputFolder / outputFileName, createdOutputStream.toByteArray)

          // Return Generated Files (for further processing)
          outputFolder / outputFileName
        }
    }


  val wro4jSettings = inConfig(Compile)(Seq(
    // Default WroFile
    wroFile in generateResources <<= (sourceDirectory in Compile)( _ / "webapp" / "WEB-INF" / "wro.xml"),
    // Default ContextFolder
    contextFolder in generateResources <<= (sourceDirectory in Compile)(_ / "webapp"),
    // Default output Folder
    outputFolder in generateResources <<= (target in Compile) (_ / "webapp" / "compiled"),
    // Generate Task
    generateResources <<= lessCompilerTask,
    // Generate Resource task is invoked if compile
    compile in Compile <<= (compile in Compile) dependsOn (generateResources in Compile)
  ))


}
