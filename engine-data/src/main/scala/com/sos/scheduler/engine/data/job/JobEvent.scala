package com.sos.scheduler.engine.data.job

import com.sos.scheduler.engine.base.sprayjson.typed.{Subtype, TypedJsonFormat}
import com.sos.scheduler.engine.data.event.Event
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

/**
  * @author Joacim Zschimmer
  */
sealed trait JobEvent extends Event {
  type Key = JobPath
}

object JobEvent {
  private implicit val jobStateJsonFormat: JsonFormat[JobState] = JobState.MyJsonFormat

  implicit val jsonFormat: TypedJsonFormat[JobEvent] = TypedJsonFormat[JobEvent](
    Subtype(jsonFormat1(JobStateChanged)),
    Subtype(jsonFormat0(() ⇒ JobUnstopped)),
    Subtype(jsonFormat0(() ⇒ JobTaskQueueChanged)))
}

final case class JobStateChanged(state: JobState) extends JobEvent

case object JobUnstopped extends JobEvent

case object JobTaskQueueChanged extends JobEvent
