package com.sos.scheduler.engine.data.job

final case class TaskEndedEvent(taskId: TaskId, jobPath: JobPath, resultCode: ResultCode)
extends TaskEvent