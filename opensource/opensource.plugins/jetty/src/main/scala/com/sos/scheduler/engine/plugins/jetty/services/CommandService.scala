package com.sos.scheduler.engine.plugins.jetty.services

import com.sos.scheduler.engine.kernel.scheduler.SchedulerXmlCommandExecutor
import com.sos.scheduler.engine.plugins.jetty.SchedulerSecurityRequest
import com.sos.scheduler.engine.plugins.jetty.services.CommandService._
import com.sos.scheduler.engine.plugins.jetty.services.WebServices.noCache
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Context, MediaType, Response}

@Path("command")
final class CommandService @Inject private(xmlCommandExecutor: SchedulerXmlCommandExecutor) {
  @POST
  @Consumes(Array(MediaType.TEXT_XML))
  @Produces(Array(MediaType.TEXT_XML))
  def post(
      command: String,
      @Context request: HttpServletRequest) =
    executeCommandWithSecurityLevel(command, request)

  @GET
  @Produces(Array(MediaType.TEXT_XML))
  def get(
      @DefaultValue("") @QueryParam("command") command: String,
      @Context request: HttpServletRequest) = {
    requireReadOnlyCommand(command)
    executeCommandWithSecurityLevel(command, request)
  }

  private def executeCommandWithSecurityLevel(command: String, request: HttpServletRequest ) = {
    val securityLevel = SchedulerSecurityRequest.securityLevel(request)
    val clientHost = Option(request.getHeader("x-forwarded-for")) map { _ takeWhile { _ != ',' } } getOrElse request.getRemoteHost
    val resultXml: String = xmlCommandExecutor.uncheckedExecuteXml(command, securityLevel, clientHost)
    Response.ok(resultXml).cacheControl(noCache).build()
  }
}

private object CommandService {
  /**
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.1.1">RFC 2616, section 9.1.1</a>;
   * "In particular, the convention has been established that the GET and HEAD methods SHOULD NOT have the significance of taking an action other than retrieval."
   */
  private def requireReadOnlyCommand(command: String): Unit = {
    if (!commandIsReadOnly(command)) throw new WebApplicationException(Response.Status.FORBIDDEN)
    if (command.isEmpty) throw new WebApplicationException(Response.Status.BAD_REQUEST)
    if (!command.head.isLetter)   // A command starting with a letter will be wrapped with <../>
      requireValidXml(command)
  }

  private def commandIsReadOnly(command: String) =
    (command startsWith "<show_") ||
      (s"$command " startsWith "<s ") ||
      (command startsWith "<s>") ||
      (command startsWith "<s/") ||
      (command startsWith "show_") ||
      (s"$command " startsWith "s ")

  private def requireValidXml(xmlString: String): Unit = {
    try xml.XML.loadString(xmlString)
    catch { case e: Exception ⇒ throw new WebApplicationException(Response.Status.BAD_REQUEST) }  // Better do not call C++ with invalid XML
  }
}