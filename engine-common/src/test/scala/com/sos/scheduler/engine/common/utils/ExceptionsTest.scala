package com.sos.scheduler.engine.common.utils

import com.sos.scheduler.engine.common.scalautil.Logger
import com.sos.scheduler.engine.common.utils.Exceptions._
import com.sos.scheduler.engine.common.utils.ExceptionsTest._
import java.io.IOException
import java.time.Instant
import org.junit.runner.RunWith
import org.scalatest.FreeSpec
import org.scalatest.junit.JUnitRunner
import com.sos.scheduler.engine.common.time.ScalaTime._

/**
  * @author Joacim Zschimmer
  */
@RunWith(classOf[JUnitRunner])
final class ExceptionsTest extends FreeSpec {

  "repeatUntilNoException" in {
    var i = 0
    val t = Instant.now()
    repeatUntilNoException(10.s, 10.ms) {
      i += 1
      if (i < 5) sys.error("TEST")
    }
    assert(i == 5)
    val duration = Instant.now() - t
    assert(duration >= 40.ms && duration <= 200.ms)
    intercept[IOException] {
      repeatUntilNoException(100.ms, 10.ms) { throw new IOException }
    }
  }

  "ignoreException executes code" in {
    var executed = false
    ignoreException(logger.debug) {
      executed = true
    }
    assert(executed)
  }

  "ignoreException ignores exception" in {
    val exception = new RuntimeException("TEST")
    var ignored: Throwable = null
    def onException(message: ⇒ String, t: Throwable) = ignored = t
    ignoreException(onException) {
      throw exception
    }
    assert(ignored eq exception)
  }

//  "ignoreException ignores exception, with slf4j.Logger.debug" in {
//    val slf4jLogger: slf4j.Logger = logger.delegate
//    ignoreException(slf4jLogger.debug) {
//      throw new RuntimeException
//    }
//  }

  "ignoreException ignores exception, with Logger.debug" in {
    ignoreException(logger.debug) {
      throw new RuntimeException
    }
  }

  "ignoreNonFatal ignores exception of given type, with Logger.debug" in {
    ignoreNonFatal[RuntimeException](logger.debug) {
      throw new IllegalStateException
    }
    intercept[IOException] {
      ignoreNonFatal[RuntimeException](logger.debug) {
        throw new IOException
      }
    }
  }

  "toStringWithCauses" in {
    assert(toStringWithCauses(new RuntimeException("TEST")) == "java.lang.RuntimeException: TEST")
    assert(toStringWithCauses(new RuntimeException("TEST", new IllegalStateException("STATE"))) ==
      "java.lang.RuntimeException: TEST, caused by java.lang.IllegalStateException: STATE")
  }
}

object ExceptionsTest {
  private val logger = Logger(getClass)
}