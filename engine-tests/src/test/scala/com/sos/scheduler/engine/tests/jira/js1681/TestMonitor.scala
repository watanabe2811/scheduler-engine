package com.sos.scheduler.engine.tests.jira.js1681

import com.sos.scheduler.engine.common.system.OperatingSystem.isWindows

/**
  * @author Joacim Zschimmer
  */
final class TestMonitor extends sos.spooler.Monitor_impl {

  override def spooler_process_before() = {
    if (isWindows || spooler_job.name != "monitor") {
      if (sys.env isDefinedAt "SCHEDULER_PARAM_JOB") sys.error("Environment variable SCHEDULER_PARAM_JOB is known in monitor process")
      if (sys.env isDefinedAt "SCHEDULER_PARAM_GLOBAL") sys.error("Environment variable SCHEDULER_PARAM_GLOBAL is known in monitor process")
    }
  	true
  }
}