package com.sos.scheduler.engine.plugins.newwebservice.routes

import AnyFileBasedRoute._
import akka.actor.ActorRefFactory
import com.sos.scheduler.engine.client.api.{FileBasedClient, SchedulerOverviewClient}
import com.sos.scheduler.engine.client.web.common.QueryHttp.pathQuery
import com.sos.scheduler.engine.common.sprayutils.SprayJsonOrYamlSupport._
import com.sos.scheduler.engine.common.sprayutils.SprayUtils.completeWithError
import com.sos.scheduler.engine.common.sprayutils.XmlString
import com.sos.scheduler.engine.data.event.Snapshot
import com.sos.scheduler.engine.data.filebased.{FileBasedDetailed, FileBasedOverview, TypedPath}
import com.sos.scheduler.engine.data.filebaseds.TypedPathRegister
import com.sos.scheduler.engine.data.queries.PathQuery
import com.sos.scheduler.engine.plugins.newwebservice.html.HtmlDirectives._
import com.sos.scheduler.engine.plugins.newwebservice.html.WebServiceContext
import com.sos.scheduler.engine.plugins.newwebservice.routes.SchedulerDirectives.typedPath
import com.sos.scheduler.engine.plugins.newwebservice.simplegui.YamlHtmlPage.implicits.jsonToYamlHtmlPage
import scala.concurrent.ExecutionContext
import shapeless.{::, HNil}
import spray.http.StatusCodes.BadRequest
import spray.http.Uri
import spray.json.DefaultJsonProtocol._
import spray.routing.Directives._
import spray.routing._

/**
  * @author Joacim Zschimmer
  */
trait AnyFileBasedRoute {

  protected implicit def client: FileBasedClient with SchedulerOverviewClient
  protected implicit def webServiceContext: WebServiceContext
  protected implicit def actorRefFactory: ActorRefFactory
  protected implicit def executionContext: ExecutionContext

  protected final def anyFileBasedRoute: Route =
    pathPrefix(PathTypeMatcher) { typedPathCompanion ⇒
      fileBasedRoute[TypedPath](typedPathCompanion)
    }

  protected final def fileBasedRoute[P <: TypedPath: TypedPath.Companion]: Route =
    parameter("return" ? "FileBasedOverview") {

      case returnType @ ("FileBasedOverview" | "FileBasedDetailed")  ⇒
        pathQuery[P].apply {
          case q: PathQuery.SinglePath ⇒
            returnType match {
              case "FileBasedOverview" ⇒ completeTryHtml(client.fileBased[P, FileBasedOverview](q.as[P]))
              case "FileBasedDetailed" ⇒ completeTryHtml(client.fileBased[P, FileBasedDetailed](q.as[P]))
              case _ ⇒ reject
            }
          case q ⇒
            returnType match {
              case "FileBasedOverview" ⇒ completeTryHtml(client.fileBaseds[P, FileBasedOverview](q))
              case "FileBasedDetailed" ⇒ completeTryHtml(client.fileBaseds[P, FileBasedDetailed](q))
              case _ ⇒ reject
            }
        }

      case "FileBasedSource" ⇒
        typedPath(implicitly[TypedPath.Companion[P]]) { typedPath ⇒
          val future =
            for (snapshot ← client.fileBased[P, FileBasedDetailed](typedPath)) yield
              for (detailed ← snapshot) yield
                for (xml ← detailed.sourceXml) yield
                  XmlString(xml)
          onSuccess(future) {
            case Snapshot(_, Some(xmlString)) ⇒
              complete(xmlString)
            case _ ⇒
              completeWithError(BadRequest, "Object has no XML source")
          }
        }

      case _ ⇒
        reject
    }
}

object AnyFileBasedRoute {
  private val PathTypeMatcher = new PathMatcher[TypedPath.Companion[TypedPath] :: HNil] {
    def apply(path: Uri.Path) =
      path match {
        case Uri.Path.Empty ⇒
          PathMatcher.Unmatched
        case _ ⇒
          TypedPathRegister.lowerCaseCamelNameToCompanion.get(path.head.toString) match {
            case Some(companion) ⇒
              PathMatcher.Matched(path.tail, companion.asInstanceOf[TypedPath.Companion[TypedPath]] :: HNil)
            case None ⇒
              PathMatcher.Unmatched
          }
      }
  }
}