package com.sos.scheduler.engine.common.process.windows

import java.util.concurrent.atomic.AtomicBoolean

/**
  * Once `releaseAfterUse` has been called and no one uses the resource, `release` ist called.
  * Thread-safe.
  *
  * @author Joacim Zschimmer
  */
private[windows] abstract class ResourceGuard[A](resource: A) {

  private var usage = 1
  private val _releaseAfterUse = new AtomicBoolean

  protected def release(resource: A): Unit

  final def apply[B](body: Option[A] ⇒ B): B =
    if (increment() > 0)
      try body(Some(resource))
      finally decrement()
    else
      body(None)

  final def releaseAfterUse(): Unit = {
    if (!_releaseAfterUse.getAndSet(true)) {
      decrement()
    }
  }

  private def increment(): Int =
    synchronized {
      if (usage > 0) {  // We don't increment 0. 0 means released.
        usage += 1
      }
      usage
    }

  private def decrement(): Unit =
    synchronized {
      assert(usage > 0)
      usage -= 1
      usage
    } match {
      case 0 ⇒ release(resource)
      case _ ⇒
    }
}
