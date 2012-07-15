package com.bowlingx.sbt.plugins

import sbt._
import Keys._
import ro.isdc.wro.manager.factory.standalone.{InjectableContextAwareManagerFactory, StandaloneContext}
import ro.isdc.wro.extensions.manager.standalone.ExtensionsStandaloneManagerFactory
import ro.isdc.wro.config._
import org.mockito.Mockito
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ro.isdc.wro.http.support.DelegatingServletOutputStream
import java.io.ByteArrayOutputStream
import javax.servlet.FilterConfig

/**
 * A Wro4j Plugin
 */

object Wro4jPlugin extends Plugin {

  object Wro4jKeys {
    val generateResources = TaskKey[Seq[File]]("generate")
  }

  import Wro4jKeys._


  private def lessCompilerTask =
    (streams, sourceDirectory in generateResources) map {
      (out, sourcesDir) =>


        val request = Mockito.mock(classOf[HttpServletRequest])
        Mockito.when(request.getRequestURI).thenReturn("")

        val response = Mockito.mock(classOf[HttpServletResponse])
        val resultOutputStream = new ByteArrayOutputStream()
        Mockito.when(response.getOutputStream).thenReturn(new DelegatingServletOutputStream(resultOutputStream))
        val config = Context.get().getConfig

        Context.set(Context.webContext(request, response, Mockito.mock(classOf[FilterConfig])), config)

        val context = new StandaloneContext()
        context.setIgnoreMissingResources(true)
        context.setWroFile(sourcesDir)
        val managerFactory = new InjectableContextAwareManagerFactory(
          new ExtensionsStandaloneManagerFactory())

        managerFactory.initialize(context)
        managerFactory.create().process()

         out.log.info("Generating Web Resources")
        Seq.empty[File]
        }


  val wro4jSettings = inConfig(Compile)(Seq(
    generateResources <<= lessCompilerTask,
    compile in Compile <<= (compile in Compile) dependsOn (generateResources in Compile)
  ))


}
