package com.sos.scheduler.engine.test.util.time

import com.sos.scheduler.engine.common.time.ScalaJoda._
import org.joda.time.Instant.now
import org.joda.time.{Duration, DateTimeUtils}

object WaitForCondition {

  /** Wartet längstens t.timeout in Schritten von t.step, bis condition wahr wird.
    * condition wird bei t.timeout > 0 wenigsten zweimal aufgerufen: am Anfang und am Ende.
    * @return letztes Ergebnis von condition */
  def waitForCondition(timeout: Duration, step: Duration)(condition: => Boolean): Boolean =
    waitForCondition(TimeoutWithSteps(timeout, step))(condition)

  /** Wartet längstens t.timeout in Schritten von t.step, bis condition wahr wird.
    * condition wird bei t.timeout > 0 wenigsten zweimal aufgerufen: am Anfang und am Ende.
    * @return letztes Ergebnis von condition */
  def waitForCondition(t: TimeoutWithSteps)(condition: => Boolean) =
    waitAtInstantsFor(t.toMillisInstantIterator(now()))(condition)

  /** Wartet bis zu den Zeitpunkten, bis condition wahr wird.
    * condition wird am Anfang und am Ende geprüft.
    * @return letztes Ergebnis von condition */
  def waitAtInstantsFor(instants: TraversableOnce[Long])(condition: => Boolean) =
    realTimeIterator(instants) exists {_ => condition}

  /** Ein Iterator, der bei next() (oder hasNext) auf den nächsten Zeitpunkt wartet.
    * Wenn aufeinanderfolgende Zeitpunkte schon erreicht sind, kehrt der Iterator trotzdem jedesmal zurück.
    * Dass kann zu überflüssigen Aktionen des Aufrufers führen (aufeinanderfolgende Prüfung einer aufwändigen Bedingung). */
  def realTimeIterator(instants: TraversableOnce[Long]): Iterator[Unit] =
    instants.toIterator map sleepUntil // toIterator führt dazu, das now() erst bei next() oder hasNext lazy aufgerufen wird.

  private[time] def sleepUntil(until: Long) {
    val w = until - DateTimeUtils.currentTimeMillis()
    if (w > 0) sleep(w)
  }
}