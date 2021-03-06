package com.sos.scheduler.engine.plugins.newwebservice.html

import com.sos.scheduler.engine.common.sprayutils.SprayUtils.passIf
import com.sos.scheduler.engine.data.event.Snapshot
import com.sos.scheduler.engine.plugins.newwebservice.html.HtmlPage._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import spray.http.CacheDirectives.{`max-age`, `no-cache`, `no-store`}
import spray.http.HttpHeaders.{Accept, `Cache-Control`}
import spray.http.HttpMethods.GET
import spray.http.MediaTypes.`text/html`
import spray.http.StatusCodes._
import spray.http.{HttpRequest, MediaRange, Uri}
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing.Directives._
import spray.routing._

/**
  * @author Joacim Zschimmer
  */
object HtmlDirectives {

  def dontCache: Directive0 =
    mapInnerRoute { inner ⇒
      requestInstance { request ⇒
        val header =
          if (isHtmlPreferred(request))
            `Cache-Control`(`max-age`(0))  // This allows browsers to use the cache when hitting the back button - for good user experience
          else
            `Cache-Control`(`max-age`(0), `no-store`, `no-cache`)
        respondWithHeader(header) {
          inner
        }
      }
    }

  /**
    * If HTML is requested, path ends with slash and request has no query, then redirect to path without slash, in case of typo.
    */
  def pathEndRedirectToSlash(webServiceContext: WebServiceContext): Route =
    pathEnd {
      redirectEmptyQueryBy(webServiceContext, path ⇒ Uri.Path(path.toString + "/"))
    }

  /**
    * If HTML is requested, path ends with slash and request has no query, then redirect to path without slash, in case of typo.
    */
  def pathEndElseRedirect(webServiceContext: WebServiceContext): Directive0 =
    mapInnerRoute { route ⇒
      pathEnd {
        route
      } ~
      pathSingleSlash {
        htmlPreferred(webServiceContext) {
          get {
            requestInstance { request ⇒
              passIf(request.uri.query == Uri.Query.Empty) {
                val withoutSlash = request.uri.copy(
                  scheme = "",
                  authority = Uri.Authority.Empty,
                  path = Uri.Path(request.uri.path.toString stripSuffix "/"))
                redirect(withoutSlash, TemporaryRedirect)
              }
            }
          }
        }
      }
    }

  /**
    * If HTML is requested, trailing slash is missing and request has no query, then redirect to trailing slash, in case of typo.
    */
  def getRequiresSlash(webServiceContext: WebServiceContext): Directive0 =
    mapInnerRoute { route ⇒
      get {
        redirectToSlash(webServiceContext) ~
        unmatchedPath {
          case path: Uri.Path.Slash ⇒ route
          case _ ⇒ reject
        }
      } ~
        route
    }

  /**
    * If HTML is requested, trailing slash is missing and request has no query, then redirect to trailing slash, in case of typo.
    */
  def redirectToSlash(webServiceContext: WebServiceContext): Route =
    pathEnd {
      htmlPreferred(webServiceContext) {  // The browser user may type "api/"
        requestInstance { request ⇒
          passIf(request.uri.query == Uri.Query.Empty) {
            val withSlash = request.uri.copy(
              scheme = "",
              authority = Uri.Authority.Empty,
              path = Uri.Path(request.uri.path.toString + "/"))
            redirect(withSlash, TemporaryRedirect)
          }
        }
      }
    }

  /**
    * If HTML is requested and request has no query, then redirect according to `changePath`, in case of user typo.
    */
  def redirectEmptyQueryBy(webServiceContext: WebServiceContext, changePath: Uri.Path ⇒ Uri.Path): Route =
    htmlPreferred(webServiceContext) {
      get {
        requestInstance { request ⇒
          if (request.uri.query == Uri.Query.Empty) {
            redirect(
              request.uri.copy(
                scheme = "",
                authority = Uri.Authority.Empty,
                path = changePath(request.uri.path)),
              TemporaryRedirect)
          } else
            reject
        }
      }
    }

  trait ToHtmlPage[A] {
    def apply(a: A, pageUri: Uri): Future[HtmlPage]
  }

  object ToHtmlPage {
    def apply[A](body: (A, Uri) ⇒ Future[HtmlPage]) =
      new ToHtmlPage[A] {
        def apply(a: A, uri: Uri) = body(a, uri)
      }
  }

  def completeTryHtml[A](snapshotFuture: ⇒ Future[Snapshot[A]])(
    implicit
      toHtmlPage: ToHtmlPage[Snapshot[A]],
      toResponseMarshaller: ToResponseMarshaller[Snapshot[A]],
      webServiceContext: WebServiceContext,
      executionContext: ExecutionContext): Route
  =
    htmlPreferred(webServiceContext) {
      requestUri { uri ⇒
        complete {
          for (snapshot ← snapshotFuture) yield
            toHtmlPage(snapshot, uri)
        }
      }
    } ~
      complete(snapshotFuture)

  def htmlPreferred(webServiceContext: WebServiceContext): Directive0 =
    mapInnerRoute { route ⇒
      requestInstance { request ⇒
        passIf(webServiceContext.htmlEnabled && request.method == GET && isHtmlPreferred(request)) {
          handleRejections(RejectionHandler.Default) {
            route
          }
        }
      }
    }

  private def isHtmlPreferred(request: HttpRequest): Boolean =
    request.header[Accept] exists { o ⇒ isHtmlPreferred(o.mediaRanges) }

  /**
    * Workaround for Spray 1.3.3, which weights the MediaType ordering of the UnMarshaller over the (higher) weight of more specific MediaRange.
    * <p>
    * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">https://tools.ietf.org/html/rfc7231#section-5.3.2</a>.
    */
  private def isHtmlPreferred(mediaRanges: Iterable[MediaRange]): Boolean =
    mediaRanges exists {
      case MediaRange.One(`text/html`, 1.0f) ⇒ true  // Highest priority q < 1 is not respected (and should be unusual for a browser)
      case _ ⇒ false
    }
}
