package com.sos.scheduler.engine.plugins.webservice.services

import com.sos.scheduler.engine.kernel.job.JobSubsystem
import com.sos.scheduler.engine.plugins.webservice.utils.WebServices.noCache
import javax.inject.Inject
import javax.ws.rs._
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response

@Path("jobs")
final class JobsService @Inject private(jobSubsystem: JobSubsystem) {
  @GET
  @Produces(Array(APPLICATION_JSON))
  def get() = {
    val result = jobSubsystem.visibleNames
    Response.ok(result).cacheControl(noCache).build()
  }
}