package com.sos.scheduler.engine.tests.jira.js1511

import com.sos.scheduler.engine.common.time.ScalaJoda._
import com.sos.scheduler.engine.data.job.JobPath
import com.sos.scheduler.engine.data.jobchain.JobChainPath
import com.sos.scheduler.engine.test.SchedulerTestUtils._
import com.sos.scheduler.engine.test.scalatest.ScalaSchedulerTest
import com.sos.scheduler.engine.tests.jira.js1511.JS1511IT._
import org.junit.runner.RunWith
import org.scalatest.FreeSpec
import org.scalatest.junit.JUnitRunner

/**
 * JS-1511 Pause postpones task starts and order steps.

 * @author Joacim Zschimmer
 */
@RunWith(classOf[JUnitRunner])
final class JS1511IT extends FreeSpec with ScalaSchedulerTest {

  "No pause" in {
    runJobAndWaitForEnd(ShellJobPath)
    runOrder(TestJobChainPath orderKey "1")
  }

  "Pause" in {
    scheduler executeXml <modify_spooler cmd='pause'/>
    val taskRun = runJobFuture(ShellJobPath)
    val orderRun = startOrder(TestJobChainPath orderKey "2")
    sleep(1.s)
    assert(!taskRun.started.isCompleted)
    assert(!orderRun.touched.isCompleted)
    scheduler executeXml <modify_spooler cmd='continue'/>
    awaitResults(List(taskRun.result, orderRun.result))
    assert(taskRun.started.isCompleted)
    assert(orderRun.touched.isCompleted)
  }

  "No pause again" in {
    runJobAndWaitForEnd(ShellJobPath)
    runOrder(TestJobChainPath orderKey "1")
  }
}

private object JS1511IT {
  private val ShellJobPath = JobPath("/shell")
  private val TestJobChainPath = JobChainPath("/test")
}