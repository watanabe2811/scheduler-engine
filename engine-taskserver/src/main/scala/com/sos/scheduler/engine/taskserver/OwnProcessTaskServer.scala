package com.sos.scheduler.engine.taskserver

import com.sos.scheduler.engine.base.process.ProcessSignal
import com.sos.scheduler.engine.base.process.ProcessSignal.SIGKILL
import com.sos.scheduler.engine.common.process.windows.{Logon, WindowsProcessCredentials}
import com.sos.scheduler.engine.common.scalautil.AutoClosing.autoClosing
import com.sos.scheduler.engine.common.scalautil.SetOnce
import com.sos.scheduler.engine.common.utils.Exceptions.andRethrow
import com.sos.scheduler.engine.taskserver.TaskServer.Terminated
import com.sos.scheduler.engine.taskserver.data.TaskServerArguments
import com.sos.scheduler.engine.taskserver.task.process.{JavaProcess, ProcessConfiguration, RichProcess}
import java.io.File
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Promise}
import spray.json._

/**
 * @author Joacim Zschimmer
 */
final class OwnProcessTaskServer(val arguments: TaskServerArguments, javaOptions: Seq[String], javaClasspath: String)
(implicit executionContext: ExecutionContext)
extends TaskServer {

  private val processOnce = new SetOnce[RichProcess]
  private val terminatedPromise = Promise[Terminated.type]()

  def terminated = terminatedPromise.future

  def start() = {
    val logon = for (logon ← arguments.logon) yield
      Logon(WindowsProcessCredentials.byKey(logon.credentialsKey), withUserProfile = logon.withUserProfile)
    val stdFileMap = RichProcess.createStdFiles(arguments.logDirectory,
      id = arguments.logFilenamePart,
      logon map { _.user })
    try {
      val process = JavaProcess.startJava(
        ProcessConfiguration(
          stdFileMap,
          additionalEnvironment = arguments.environment,
          agentTaskIdOption = Some(arguments.agentTaskId),
          killScriptOption = arguments.killScriptOption,
          logon = logon),
        options = javaOptions,
        classpath = Some(javaClasspath + File.pathSeparator + JavaProcess.OwnClasspath),
        mainClass = TaskServerMain.getClass.getName stripSuffix "$", // Strip Scala object class suffix
        arguments = Nil)
      processOnce := process
      process.terminated  map { _ ⇒ Terminated }onComplete terminatedPromise.complete
      try autoClosing(process.stdinWriter) {
        _.write(
          arguments.copy(
            stdFileMap = stdFileMap,
            logStdoutAndStderr = true,
            logon = None/*Already logged-on*/)
          .toJson.compactPrint)
      } catch andRethrow {
        sendProcessSignal(SIGKILL)
      }
    } catch andRethrow {
      RichProcess.tryDeleteFiles(stdFileMap.values)
    }
  }

  override def close(): Unit = {
    for (p ← processOnce) {
      // Wait for process _after_ Tunnel, registered with registerCloseable, has been closed
      try p.waitForTermination()
      finally p.close()
    }
  }

  def sendProcessSignal(signal: ProcessSignal) =
    for (p ← processOnce) p.sendProcessSignal(signal)

  def deleteLogFiles(): Unit =
    for (p ← processOnce) RichProcess.tryDeleteFiles(p.processConfiguration.stdFileMap.values)

  def pidOption = processOnce flatMap { _.pidOption }
}
