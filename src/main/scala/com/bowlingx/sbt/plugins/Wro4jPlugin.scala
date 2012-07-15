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
    val generateResources = TaskKey[Seq[File]]("generate")
  }

  import Wro4jKeys._

  private[this] def managerFactory(sourceDir:File) = {
    val context = new StandaloneContext()
    context.setIgnoreMissingResources(true)
    context.setContextFolder(sourceDir / "webapp")
    context.setWroFile(sourceDir / "webapp" / "WEB-INF" / "wro.xml")
    context.setMinimize(true)

    val managerFactory = new InjectableContextAwareManagerFactory(new ExtensionsStandaloneManagerFactory())

    managerFactory.initialize(context)
    val processor = new DefaultProcesorsFactory()
    processor.addPostProcessor(new LessCssProcessor())
    managerFactory.setProcessorsFactory(processor)
    managerFactory.create()
  }

  private def lessCompilerTask =
    (streams, sourceDirectory in generateResources) map {
      (out, sourcesDir) =>
        out.log.info("Generating Web Resources")

        Context.set(Context.standaloneContext())

        import scala.collection.JavaConversions._
        val factory = managerFactory(sourcesDir)
        for {
          suffix <- ResourceType.values()
          groupName <- factory.getModelFactory.create().getGroupNames
        } yield {
          out.log.info(groupName)
        //mock request
          val request = Mockito.mock(classOf[HttpServletRequest])
          Mockito.when(request.getRequestURI).thenReturn("%s.%s" format (groupName, suffix))
          //mock response
          val response = Mockito.mock(classOf[HttpServletResponse])
          val createdOutputStream = new ByteArrayOutputStream()
          Mockito.when(response.getOutputStream).thenReturn(new DelegatingServletOutputStream(createdOutputStream))

          //init context
          val conf = Context.get().getConfig
          Context.set(Context.webContext(request, response, Mockito.mock(classOf[FilterConfig])), conf)

          factory.process()

          out.log.info(createdOutputStream.toString)
        }

        Seq.empty[File]
    }


  val wro4jSettings = inConfig(Compile)(Seq(
    generateResources <<= lessCompilerTask,
    compile in Compile <<= (compile in Compile) dependsOn (generateResources in Compile)
  ))


}
