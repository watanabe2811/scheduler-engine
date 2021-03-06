package com.sos.scheduler.engine.kernel.scheduler;

import com.google.common.base.Charsets;
import com.sos.scheduler.engine.data.job.JobPath;
import java.nio.charset.Charset;
import java.time.Duration;
import static com.google.common.base.Charsets.ISO_8859_1;

public final class SchedulerConstants {
    public static final Charset defaultEncoding = Charsets.UTF_8;
    public static final Charset schedulerEncoding = ISO_8859_1;
    public static final Charset logFileEncoding = schedulerEncoding;
    public static final String remoteSchedulerParameterName = "scheduler.remote_scheduler";
    public static final String FileOrderPathVariableName = "scheduler_file_path";
    public static final String FileOrderAgentUriVariableName = "scheduler_file_remote_scheduler";
    public static final Duration DatabaseOrderCheckPeriod = Duration.ofSeconds(15); // spooler_order.cxx: check_database_orders_period
    public static final JobPath FileOrderSinkJobPath = new JobPath("/scheduler_file_order_sink");
    public static final JobPath ServiceForwarderJobPath = new JobPath("/scheduler_service_forwarder");


    private SchedulerConstants() {}
}
