package com.sos.scheduler.engine.tests.jira.js1642

import akka.util.ByteString
import com.google.common.io.Files.touch
import com.sos.scheduler.engine.base.sprayjson.JsonRegexMatcher._
import com.sos.scheduler.engine.base.sprayjson.SprayJson.implicits._
import com.sos.scheduler.engine.base.system.SystemInformation
import com.sos.scheduler.engine.client.api.SchedulerClient
import com.sos.scheduler.engine.client.web.StandardWebSchedulerClient
import com.sos.scheduler.engine.common.scalautil.Closers.implicits._
import com.sos.scheduler.engine.common.scalautil.FileUtils.implicits._
import com.sos.scheduler.engine.common.scalautil.Futures.implicits._
import com.sos.scheduler.engine.common.scalautil.Logger
import com.sos.scheduler.engine.common.scalautil.xmls.SafeXML
import com.sos.scheduler.engine.common.scalautil.xmls.ScalaXmls.implicits.RichXmlFile
import com.sos.scheduler.engine.common.sprayutils.JsObjectMarshallers._
import com.sos.scheduler.engine.common.time.ScalaTime._
import com.sos.scheduler.engine.common.time.Stopwatch
import com.sos.scheduler.engine.common.time.WaitForCondition.waitForCondition
import com.sos.scheduler.engine.common.utils.FreeTcpPortFinder.findRandomFreeTcpPort
import com.sos.scheduler.engine.common.utils.IntelliJUtils.intelliJuseImports
import com.sos.scheduler.engine.data.compounds.{OrderTreeComplemented, OrdersComplemented}
import com.sos.scheduler.engine.data.event.{AnyKeyedEvent, Event, EventId, KeyedEvent, Snapshot}
import com.sos.scheduler.engine.data.events.SchedulerAnyKeyedEventJsonFormat
import com.sos.scheduler.engine.data.filebased.{FileBasedActivated, FileBasedDetailed, FileBasedEvent, FileBasedState, TypedPath, UnknownTypedPath}
import com.sos.scheduler.engine.data.job.TaskId
import com.sos.scheduler.engine.data.jobchain.{EndNodeOverview, JobChainDetailed, JobChainOverview, JobChainPath, NodeId}
import com.sos.scheduler.engine.data.log.Logged
import com.sos.scheduler.engine.data.order.{OrderKey, OrderOverview, OrderStatistics, OrderStatisticsChanged, OrderStepStarted}
import com.sos.scheduler.engine.data.queries.{JobChainNodeQuery, JobChainQuery, OrderQuery, PathQuery}
import com.sos.scheduler.engine.data.scheduler.{SchedulerId, SchedulerOverview, SchedulerState}
import com.sos.scheduler.engine.data.system.JavaInformation
import com.sos.scheduler.engine.data.xmlcommands.{ModifyOrderCommand, OrderCommand}
import com.sos.scheduler.engine.kernel.DirectSchedulerClient
import com.sos.scheduler.engine.kernel.event.collector.EventCollector
import com.sos.scheduler.engine.kernel.folder.FolderSubsystemClient
import com.sos.scheduler.engine.kernel.job.TaskSubsystemClient
import com.sos.scheduler.engine.kernel.variable.SchedulerVariableSet
import com.sos.scheduler.engine.test.EventBusTestFutures.implicits.RichEventBus
import com.sos.scheduler.engine.test.SchedulerTestUtils.jobChainOverview
import com.sos.scheduler.engine.test.configuration.{HostwareDatabaseConfiguration, InMemoryDatabaseConfiguration, TestConfiguration}
import com.sos.scheduler.engine.test.scalatest.ScalaSchedulerTest
import com.sos.scheduler.engine.tests.jira.js1642.Data._
import com.sos.scheduler.engine.tests.jira.js1642.JS1642IT._
import java.nio.file.Files.deleteIfExists
import org.junit.runner.RunWith
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import scala.collection.{immutable, mutable}
import scala.concurrent.{ExecutionContext, Future}
import spray.http.MediaTypes.{`text/html`, `text/richtext`}
import spray.http.StatusCodes.{InternalServerError, NotAcceptable, NotFound}
import spray.httpx.UnsuccessfulResponseException
import spray.json._

/**
  * JS-1642 WebSchedulerClient and NewWebServicePlugin.
  *
  * @author Joacim Zschimmer
  */
@RunWith(classOf[JUnitRunner])
final class JS1642IT extends FreeSpec with ScalaSchedulerTest with SpeedTests {

  private lazy val httpPort = findRandomFreeTcpPort()
  protected lazy val directSchedulerClient = instance[DirectSchedulerClient]
  protected lazy val webSchedulerClient = new StandardWebSchedulerClient(s"http://127.0.0.1:$httpPort").closeWithCloser
  protected override lazy val testConfiguration = TestConfiguration(getClass,
    mainArguments = List(s"-http-port=$httpPort", "-distributed-orders", "-suppress-watchdog-thread"),
    database = Some(
      if (sys.props contains "test.mysql")
        HostwareDatabaseConfiguration("jdbc -class=com.mysql.jdbc.Driver -user=jobscheduler -password=jobscheduler jdbc:mysql://127.0.0.1/jobscheduler")
      else
        InMemoryDatabaseConfiguration))
  private implicit lazy val executionContext = instance[ExecutionContext]
  private lazy val taskSubsystem = instance[TaskSubsystemClient]
  private lazy val eventCollector = instance[EventCollector]
  private val orderKeyToTaskId = mutable.Map[OrderKey, TaskId]()

  private object barrier {
    lazy val file = testEnvironment.tmpDirectory / "TEST-BARRIER"

    def touchFile() = {
      val variableSet = instance[SchedulerVariableSet]
      variableSet(TestJob.BarrierFileVariableName) = file.toString
      touch(file)
      onClose { deleteIfExists(file) }
    }
  }

  private val setting = List[(String, () ⇒ SchedulerClient)](
    "DirectSchedulerClient" → { () ⇒ directSchedulerClient },
    "WebSchedulerClient" → { () ⇒ webSchedulerClient })

  override protected def onSchedulerActivated() = {
    super.onSchedulerActivated()
    eventReader.start()
    barrier.touchFile()
    scheduler executeXml OrderCommand(aAdHocOrderKey, at = Some(OrderStartAt), suspended = Some(true))
    scheduler executeXml OrderCommand(xbAdHocDistributedOrderKey, at = None)
    startOrderProcessing()
    testEnvironment.fileFromPath(b1OrderKey).append(" ")
    instance[FolderSubsystemClient].updateFolders()   // Replacement is pending
  }

  override def afterAll() = {
    eventReader.stop()
    super.afterAll()
  }

  private object eventReader {
    val directEvents = mutable.Buffer[AnyKeyedEvent]()
    val webEvents = mutable.Buffer[AnyKeyedEvent]()
    private var stopping = false
    private var activatedEventId = EventId.BeforeFirst

    def start(): Unit = {
      activatedEventId = eventCollector.lastEventId  // Web events before Scheduler activation are ignored
      eventBus.onHot[Event] {
        case event if SchedulerAnyKeyedEventJsonFormat canSerialize event ⇒
          if (isPermitted(event)) {
            directEvents += event
          }
      }
      start(EventId.BeforeFirst)
    }

    def stop(): Unit = {
      stopping = true
    }

    private def start(after: EventId): Unit = {
      (for (Snapshot(_, eventSnapshots) ← webSchedulerClient.events[Event](after).appendCurrentStackTrace) yield {
        this.webEvents ++= eventSnapshots filter { snapshot ⇒ snapshot.eventId > activatedEventId && isPermitted(snapshot.value) } map { _.value }
        if (!stopping) {
          start(after = eventSnapshots.last.eventId)
        }
      })
      .failed foreach { throwable ⇒
        if (!stopping) {
          logger.error(s"webSchedulerClient.events: $throwable", throwable)
          controller.terminateAfterException(throwable)
        }
      }
    }

    private def isPermitted(event: AnyKeyedEvent) =
      event match {
        case KeyedEvent(_, FileBasedActivated) ⇒ this.webEvents.nonEmpty   // directEvents miss activation events at start
        case KeyedEvent(_, e: Logged) ⇒ false
        case _ ⇒ true
      }

    def check() = {
      assert(directEvents.nonEmpty)
      waitForCondition(5.s, 100.ms) { webEvents.size == directEvents.size }
      assert(webEvents == directEvents)
      val untypedPathDirectEvents = directEvents map {
        case KeyedEvent(key: TypedPath, e: FileBasedEvent) ⇒ KeyedEvent(e)(key.asTyped[UnknownTypedPath])  // Trait TypedPath is not properly deserializable
        case o ⇒ o
      }
      assert(webEvents == untypedPathDirectEvents)
    }
  }

  private def startOrderProcessing() = {
    val expectedTaskIds = ProcessableOrderKeys.indices map { i ⇒ TaskId.First + i }
    for ((orderKey, expectedTaskId) ← ProcessableOrderKeys zip expectedTaskIds) {
      val event = eventBus.awaiting[OrderStepStarted](orderKey) {
        scheduler executeXml ModifyOrderCommand(orderKey, suspended = Some(false))
      }
      assert(event.taskId == expectedTaskId)
      orderKeyToTaskId(orderKey) = event.taskId
    }
  }

  lazy val data = new Data(
    taskIdToStartedAt = (for (taskId ← 3 to 5 map TaskId.apply) yield taskId → taskSubsystem.task(taskId).processStartedAt.get).toMap)
  import data._

  "overview" in {
    val overview = fetchWebAndDirectEqualized[SchedulerOverview](
      _.overview,
      _.copy(system = SystemInformation.ForTest, java = JavaInformation.ForTest))
    assert(overview.schedulerId == SchedulerId("test"))
    assert(overview.state == SchedulerState.running)
  }

  "FileBasedDetailed" in {
    val fileBasedDetailed = fetchWebAndDirectEqualized[FileBasedDetailed](
      _.fileBasedDetailed(a1OrderKey),
      _.copy(sourceXml = None))
    val file = testEnvironment.fileFromPath(a1OrderKey)
    assert(SafeXML.loadString(fileBasedDetailed.sourceXml.get) == file.xml)
    assert(fileBasedDetailed.fileModifiedAt.isDefined)
  }

  "orders[OrderOverview]" in {
    val orders: immutable.Seq[OrderOverview] = fetchWebAndDirect {
      _.orders[OrderOverview]
    }
    assert((orders.toVector.sorted map normalizeOrderOverview) == ExpectedOrderOverviews)
  }

  "ordersComplemented" in {
    val ordersComplemented: OrdersComplemented[OrderOverview] = fetchWebAndDirect {
      _.ordersComplemented[OrderOverview]
    }
    assert(ordersComplemented.copy(orders = ordersComplemented.orders map normalizeOrderOverview) ==
      ExpectedOrdersComplemented)
  }

  "orderTreeComplemented" in {
    val treeOverview: OrderTreeComplemented[OrderOverview] = fetchWebAndDirect {
      _.orderTreeComplementedBy[OrderOverview](OrderQuery.All)
    }
    assert(treeOverview.copy(orderTree = treeOverview.orderTree mapLeafs normalizeOrderOverview) ==
      ExpectedOrderTreeComplemented)
  }

  "ordersComplementedBy isSuspended" in {
    val orderQuery = OrderQuery(isSuspended = Some(true))
    val ordersComplemented: OrdersComplemented[OrderOverview] = fetchWebAndDirect {
      _.ordersComplementedBy[OrderOverview](orderQuery)
    }
    assert(ordersComplemented == ExpectedSuspendedOrdersComplemented)
  }

  "ordersComplementedBy query /aJobChain" in {
    val query = OrderQuery(JobChainQuery(PathQuery[JobChainPath]("/aJobChain")))
    val ordersComplemented: OrdersComplemented[OrderOverview] = fetchWebAndDirect {
      _.ordersComplementedBy[OrderOverview](query)
    }
    assert((ordersComplemented.orders map { _.orderKey }).toSet == Set(a1OrderKey, a2OrderKey, aAdHocOrderKey))
  }

  "ordersComplementedBy query /aJobChain/ throws SCHEDULER-161" in {
    val query = OrderQuery(JobChainQuery(PathQuery[JobChainPath]("/aJobChain/")))
    intercept[RuntimeException] {
      fetchWebAndDirect {
        _.ordersComplementedBy[OrderOverview](query)
      }
    } .getMessage should include ("SCHEDULER-161")
  }

  "ordersComplementedBy query /xFolder/" in {
    val orderQuery = OrderQuery(JobChainQuery(PathQuery[JobChainPath]("/xFolder/")))
    val ordersComplemented = fetchWebAndDirect[OrdersComplemented[OrderOverview]] {
      _.ordersComplementedBy[OrderOverview](orderQuery)
    }
    assert((ordersComplemented.orders map { _.orderKey }).toSet == Set(xa1OrderKey, xa2OrderKey, xb1OrderKey, xbAdHocDistributedOrderKey))
  }

  "ordersComplementedBy query /xFolder throws SCHEDULER-161" in {
    val query = OrderQuery(JobChainQuery(PathQuery[JobChainPath]("/xFolder")))
    intercept[RuntimeException] {
      fetchWebAndDirect {
        _.ordersComplementedBy[OrderOverview](query)
      }
    } .getMessage should include ("SCHEDULER-161")
  }

  "orders query OrderId" in {
    val orderQuery = OrderQuery(orderIds = Some(Set(OneOrderId)))
    val orders: immutable.Seq[OrderOverview] = fetchWebAndDirect {
      _.ordersBy[OrderOverview](orderQuery)
    }
    assert((orders map { _.orderKey }).toSet == Set(a1OrderKey, b1OrderKey, xa1OrderKey, xb1OrderKey))
  }

  "orders single non-existent, non-distributed OrderKey throws exception" in {
    assert(!jobChainOverview(aJobChainPath).isDistributed)
    checkUnknownOrderKeyException(aJobChainPath orderKey "UNKNOWN")
  }

  "orders single non-existent, distributed OrderKey throws exception" in {
    assert(jobChainOverview(xbJobChainPath).isDistributed)
    checkUnknownOrderKeyException(xbJobChainPath orderKey "UNKNOWN")
  }

  "orders query JobPath" in {
    val orderQuery = OrderQuery(JobChainNodeQuery(jobPaths = Some(Set(TestJobPath))))
    val orders: immutable.Seq[OrderOverview] = fetchWebAndDirect {
      _.ordersBy[OrderOverview](orderQuery)
    }
    assert((orders map { _.orderKey }).toSet == Set(a1OrderKey, a2OrderKey, aAdHocOrderKey, b1OrderKey))
  }

  "orders query JobPath of non-existent job, distributed" in {
    val orderQuery = OrderQuery(JobChainNodeQuery(jobPaths = Some(Set(XTestBJobPath))))
    val orders: immutable.Seq[OrderOverview] = fetchWebAndDirect {
      _.ordersBy[OrderOverview](orderQuery)
    }
    assert((orders map { _.orderKey }).toSet == Set(xb1OrderKey, xbAdHocDistributedOrderKey))
  }

  def checkUnknownOrderKeyException(orderKey: OrderKey): Unit = {
    val orderQuery = OrderQuery().withOrderKey(orderKey)
    intercept[RuntimeException] {
      fetchWebAndDirect {
        _.ordersBy[OrderOverview](orderQuery)
      }
    } .getMessage should include ("SCHEDULER-161")
  }

  "jobChainOverview All" in {
    val jobChainOverviews: immutable.Seq[JobChainOverview] = fetchWebAndDirect {
      _.jobChainOverviewsBy(JobChainQuery.All)
    }
    assert(jobChainOverviews.toSet == Set(
      JobChainOverview(aJobChainPath, FileBasedState.active, isDistributed = false),
      JobChainOverview(bJobChainPath, FileBasedState.active, isDistributed = false),
      xaJobChainOverview,
      xbJobChainOverview))
  }

  "jobChainOverview query" in {
    val query = JobChainQuery(PathQuery[JobChainPath]("/xFolder/"))
    val jobChainOverviews: immutable.Seq[JobChainOverview] = fetchWebAndDirect {
      _.jobChainOverviewsBy(query)
    }
    assert(jobChainOverviews.toSet == Set(
      xaJobChainOverview,
      xbJobChainOverview))
  }

  "jobChainDetailed" in {
    val jobChainDetailed: JobChainDetailed = fetchWebAndDirect {
      _.jobChainDetailed(xaJobChainPath)
    }
    assert(jobChainDetailed ==
      JobChainDetailed(
        xaJobChainOverview,
        List(
          Xa100NodeOverview,
          EndNodeOverview(
            xaJobChainPath,
            NodeId("END")))))
  }

  "orderStatistics" - {
    def testAllOrderStatistics() {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainQuery.All)
      }
      assert(orderStatistics == OrderStatistics(
        total = 8,
        notPlanned = 0,
        planned = 1,
        due = 4,
        started = 3,
        inTask = 3,
        inProcess = 3,
        setback = 0,
        suspended = 2,
        blacklisted = 0,
        permanent = 6,
        fileOrder = 0))
    }

    "/" in {
      testAllOrderStatistics()
    }

    val parallelFactor = 1000
    s"$parallelFactor simultaneously requests" in {
      (for (_ ← 1 to parallelFactor) yield Future { testAllOrderStatistics() }) await TestTimeout
    }

    s"$xFolderPath" in {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainQuery(PathQuery(xFolderPath)))
      }
      assert(orderStatistics == OrderStatistics(
        total = 4,
        notPlanned = 0,
        planned = 0,
        due = 4,
        started = 0,
        inTask = 0,
        inProcess = 0,
        setback = 0,
        suspended = 1,
        blacklisted = 0,
        permanent = 3,
        fileOrder = 0))
    }

    s"NodeId 100" in {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainNodeQuery(nodeIds = Some(Set(NodeId("100")))))
      }
      assert(orderStatistics == OrderStatistics(
        total = 8,
        notPlanned = 0,
        planned = 1,
        due = 4,
        started = 3,
        inTask = 3,
        inProcess = 3,
        setback = 0,
        suspended = 2,
        blacklisted = 0,
        permanent = 6,
        fileOrder = 0))
    }

    s"NodeId 200" in {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainNodeQuery(nodeIds = Some(Set(NodeId("200")))))
      }
      assert(orderStatistics == OrderStatistics.Zero)
    }

    s"Job /test" in {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainNodeQuery(jobPaths = Some(Set(TestJobPath))))
      }
      assert(orderStatistics == OrderStatistics(
        total = 4,
        notPlanned = 0,
        planned = 1,
        due = 0,
        started = 3,
        inTask = 3,
        inProcess = 3,
        setback = 0,
        suspended = 1,
        blacklisted = 0,
        permanent = 3,
        fileOrder = 0))
    }

    s"Job /xFolder/test-b, distributed" in {
      val orderStatistics: OrderStatistics = fetchWebAndDirect {
        _.orderStatistics(JobChainNodeQuery(jobPaths = Some(Set(XTestBJobPath))))
      }
      assert(orderStatistics == OrderStatistics(
        total = 2,
        notPlanned = 0,
        planned = 0,
        due = 2,
        started = 0,
        inTask = 0,
        inProcess = 0,
        setback = 0,
        suspended = 0,
        blacklisted = 0,
        permanent = 1,
        fileOrder = 0))
    }
  }

  def fetchWebAndDirect[A](body: SchedulerClient ⇒ Future[Snapshot[A]]): A =
    fetchWebAndDirectEqualized[A](body, identity)

  def fetchWebAndDirectEqualized[A](body: SchedulerClient ⇒ Future[Snapshot[A]], equalize: A ⇒ A): A = {
    val a: A = awaitContent(body(webSchedulerClient))
    assert(equalize(a) == equalize(awaitContent(body(directSchedulerClient))))
    a
  }

  for ((testGroup, getClient) ← setting) testGroup - {
    lazy val client = getClient()

    "command" - {
      "<show_state> as xml.Elem" in {
        testValidExecute {
          client.execute(<show_state/>)
        }
        testThrowingExecute {
          client.execute(<UNKNOWN/>)
        }
        testValidExecute {
          client.uncheckedExecute(<show_state/>)
        }
        testUncheckedExecute {
          client.uncheckedExecute(<UNKNOWN/>)
        }
      }

      "<show_state> as String" in {
        testValidExecute {
          client.executeXml("<show_state/>")
        }
        testValidExecute {
          client.uncheckedExecuteXml("<show_state/>")
        }
        testThrowingExecute {
          client.executeXml("<UNKNOWN/>")
        }
        testUncheckedExecute {
          client.uncheckedExecuteXml("<UNKNOWN/>")
        }
      }

      "<show_state> as ByteString" in {
        testValidExecute {
          client.executeXml(ByteString("<show_state/>"))
        }
        testValidExecute {
          client.uncheckedExecuteXml(ByteString("<show_state/>"))
        }
        testThrowingExecute {
          client.executeXml(ByteString("<UNKNOWN/>"))
        }
        testUncheckedExecute {
          client.uncheckedExecuteXml(ByteString("<UNKNOWN/>"))
        }
      }

      def testValidExecute(execute: ⇒ Future[String]): Unit = {
        val response = execute map SafeXML.loadString await TestTimeout
        val state = response \ "answer" \ "state"
        assert((state \ "@state").toString == "running")
        assert((state \ "@ip_address").toString == "127.0.0.1")
      }

      def testThrowingExecute(execute: ⇒ Future[String]): Unit =
        intercept[RuntimeException] {
          controller.toleratingErrorCodes(_ ⇒ true) {
            execute await TestTimeout
          }
        }

      def testUncheckedExecute(execute: ⇒ Future[String]): Unit = {
        val response = controller.toleratingErrorCodes(_ ⇒ true) {
          execute map SafeXML.loadString await TestTimeout
        }
        assert((response \ "answer" \ "ERROR" \ "@code").toString.nonEmpty)
      }

      "Speed test <show_state>" in {
        Stopwatch.measureTime(10, "<show_state what='orders'>") {
          client.executeXml("<show_state what='orders'/>") map SafeXML.loadString await TestTimeout
        }
      }
    }
  }

  "GET" - {
    "getOrdersComplementedBy isSuspended" in {
      val orderQuery = OrderQuery(isSuspended = Some(true))
      val ordersComplemented = awaitContent(webSchedulerClient.getOrdersComplementedBy[OrderOverview](orderQuery))
      assert(ordersComplemented == awaitContent(directSchedulerClient.ordersComplementedBy[OrderOverview](orderQuery)))
      assert(ordersComplemented == ExpectedSuspendedOrdersComplemented)
    }
  }

  "JSON" - {
    "overview" in {
      val overviewString = webSchedulerClient.get[String](_.overview) await TestTimeout
      testRegexJson(
        json = overviewString,
        patternMap = Map(
          "eventId" → AnyLong,
          "version" → """\d+\..+""".r,
          "version" → """.+""".r,
          "startedAt" → AnyIsoTimestamp,
          "schedulerId" → "test",
          "httpPort" → httpPort,
          "pid" → AnyInt,
          "state" → "running",
          "system" → AnyRef,
          "java" → AnyRef))
    }

    "orderOverviews" in {
      val snapshot = webSchedulerClient.get[JsObject](_.order[OrderOverview]) await TestTimeout
      assert((snapshot("orders").asJsArray map normalizeOrderOverviewJson) == ExpectedOrderOverviewsJsArray)
    }

    "ordersComplemented" in {
      val ordersComplemented = webSchedulerClient.get[JsObject](_.order.complemented[OrderOverview]()) await TestTimeout
      val orderedOrdersComplemented = JsObject((ordersComplemented.fields - Snapshot.EventIdJsonName) ++ Map(
        "orders" → (ordersComplemented("orders").asJsArray map normalizeOrderOverviewJson),
        "usedTasks" → ordersComplemented("usedTasks").asJsArray,
        "usedJobs" → ordersComplemented("usedJobs").asJsArray))
      assert(orderedOrdersComplemented == ExpectedOrdersOrdersComplementedJsObject)
    }

    "orderTreeComplemented" in {
      val tree = webSchedulerClient.get[JsObject](_.order.treeComplemented[OrderOverview]) await TestTimeout
      val normalized = JsObject(tree.fields - Snapshot.EventIdJsonName) deepMapJsObjects normalizeOrderOverviewJson
      assert(normalized == ExpectedOrderTreeComplementedJsObject)
    }
  }

  "Unknown Accept content type is rejected" - {
    "overview" in {
      intercept[UnsuccessfulResponseException] {
        webSchedulerClient.get[String](_.overview, accept = `text/richtext`) await TestTimeout
      }.response.status shouldEqual NotAcceptable
    }
  }

  "text/html" - {  // Inofficial
    "overview" in {
      val html = webSchedulerClient.get[String](_.overview, accept = `text/html`) await TestTimeout
      assert(html startsWith "<!DOCTYPE html")
      assert(html endsWith "</html>")
      assert(html contains "JobScheduler")
      assert(html contains "Started at")
    }

    "order.ordersComplemented" in {
      val html = webSchedulerClient.get[String](_.order.complemented[OrderOverview](), accept = `text/html`) await TestTimeout
      assert(html startsWith "<!DOCTYPE html")
      assert(html endsWith "</html>")
      assert(html contains "JobScheduler")
    }
  }

  "WebSchedulerClient.getByUri" in {
    val jsObject = webSchedulerClient.getByUri[JsObject]("api") await TestTimeout
    val Snapshot(_, directOverview) = directSchedulerClient.overview await TestTimeout
    assert(jsObject.fields("version") == JsString(directOverview.version))
  }

  "Web service error behavior" - {
    "jobscheduler/master/api/ERROR-500" in {
      intercept[UnsuccessfulResponseException] { webSchedulerClient.get[String](_.uriString(s"TEST/ERROR-500")) await TestTimeout }
        .response.status shouldEqual InternalServerError
    }

    "jobscheduler/master/api/UNKNOWN" in {
      intercept[UnsuccessfulResponseException] { webSchedulerClient.get[String](_.uriString("TEST/UNKNOWN")) await TestTimeout }
        .response.status shouldEqual NotFound
    }
  }

  "OrderStatisticsChanged" in {
    val aSnapshot = webSchedulerClient.events[OrderStatisticsChanged](after = EventId.BeforeFirst) await TestTimeout
    val aStatistics = aSnapshot.value.head.value.event.orderStatistics

    val bFuture = webSchedulerClient.events[OrderStatisticsChanged](after = aSnapshot.eventId)
    scheduler executeXml ModifyOrderCommand(aAdHocOrderKey, suspended = Some(false))
    val bSnapshot = bFuture await TestTimeout
    val bStatistics = bSnapshot.value.head.value.event.orderStatistics
    assert(bStatistics == aStatistics.copy(suspended = aStatistics.suspended - 1))

    val cFuture = webSchedulerClient.events[OrderStatisticsChanged](after = bSnapshot.eventId)
    scheduler executeXml ModifyOrderCommand(aAdHocOrderKey, suspended = Some(true))
    val cSnapshot = cFuture await TestTimeout
    val cStatistics = cSnapshot.value.head.value.event.orderStatistics
    assert(cStatistics == aStatistics)
  }

  "events" in {
    eventReader.check()
  }

  for ((testGroup, getClient) ← setting) testGroup - {
    lazy val client = getClient()

    "orders[OrderOverview] speed" in {
      Stopwatch.measureTime(50, s""""orderOverviews with $OrderCount orders"""") {
        client.orders[OrderOverview] await TestTimeout
      }
    }

    "ordersComplemented speed" in {
      Stopwatch.measureTime(50, "ordersComplemented") {
        client.ordersComplemented[OrderOverview] await TestTimeout
      }
    }
  }

  addOptionalSpeedTests()

  private def awaitContent[A](future: Future[Snapshot[A]]): A =
    (future await TestTimeout).value
}

object JS1642IT {
  intelliJuseImports(JsObjectMarshaller)
  private val logger = Logger(getClass)

  private def normalizeOrderOverview(o: OrderOverview) = o.copy(startedAt = None)

  private def normalizeOrderOverviewJson(o: JsValue) = JsObject(o.asJsObject.fields - "startedAt")
}