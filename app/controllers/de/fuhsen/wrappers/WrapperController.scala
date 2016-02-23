package controllers.de.fuhsen.wrappers

import javax.inject.Inject

import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsString, JsArray}
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, Controller, Result}

import scala.concurrent.Future

/**
 * Handles requests to API wrappers. Wrappers must at least implement [[RestApiWrapperTrait]].
 */
class WrapperController @Inject()(ws: WSClient) extends Controller {
  def search(wrapperId: String, query: String) = Action.async {
    WrapperController.wrapperMap.get(wrapperId) match {
      case Some(wrapper) =>
        Logger.info(s"Starting $wrapperId Search with query: " + query)
        val apiRequest = createApiRequest(wrapper, query)
        val apiResponse = executeApiRequest(apiRequest)
        transformApiResponse(wrapper, apiResponse)
      case None =>
        Future(NotFound("Wrapper " + wrapperId + " not found! Supported wrapper: " +
            WrapperController.wrapperMap.keys.mkString(", ")))
    }
  }

  /** Handles transformation if configured for the wrapper */
  private def transformApiResponse(wrapper: RestApiWrapperTrait,
                                   apiResponse: Future[Either[String, Result]]): Future[Result] = {
    apiResponse flatMap {
      case Right(errorResult) =>
        // There has been an error previously, don't go on.
        Future(errorResult)
      case Left(content) =>
        handleSilkTransformation(wrapper, content)
    }
  }

  /** Executes the request to the wrapped REST API */
  private def executeApiRequest(apiRequest: WSRequest): Future[Either[String, Result]] = {
    apiRequest.get.map { resp =>
      if (resp.status >= 400) {
        Right(InternalServerError("There was a problem with the wrapper or the wrapped service. Service response:\n\n" + resp.body))
      } else if (resp.status >= 300) {
        Right(InternalServerError("Wrapper seems to be configured incorrectly, received a redirect from wrapped service."))
      } else {
        Left(resp.body)
      }
    }
  }

  /** If transformations are configured then execute them via the Silk REST API */
  def handleSilkTransformation(wrapper: RestApiWrapperTrait,
                               content: String,
                               acceptType: String = "application/ld+json"): Future[Result] = {
    wrapper match {
      case silkTransform: SilkTransformableTrait =>
        val transformRequest = ws.url(silkTransform.transformationEndpoint)
            .withHeaders("Content-Type" -> "application/xml")
            .withHeaders("Accept" -> acceptType)
        val response = transformRequest.post(silkTransform.silkTransformationRequestBody(content)) map { response =>
          if (response.status >= 400) {
            InternalServerError("There was a problem with the wrapper or the Silk transformation endpoint. Service response:\n\n" + response.body)
          } else if (response.status >= 300) {
            InternalServerError("Wrapper seems to be configured incorrectly, received a redirect from Silk transformation endpoint.")
          } else {
            Ok(response.body)
          }
        }
        response
      case _ =>
        // No transformation to be executed
        Future(Ok(content))
    }
  }

  /** Creates the complete API REST request and executes it asynchronously. */
  def createApiRequest(wrapper: RestApiWrapperTrait, query: String): WSRequest = {
    val apiRequest: WSRequest = ws.url(wrapper.apiUrl)
    val apiRequestWithApiParams = addQueryParameters(wrapper, apiRequest, query)
    val apiRequestWithOAuthIfNeeded = handleOAuth(wrapper, apiRequestWithApiParams)
    apiRequestWithOAuthIfNeeded
  }

  /** Add all query parameters to the request. */
  private def addQueryParameters(wrapper: RestApiWrapperTrait,
                                 request: WSRequest,
                                 queryString: String): WSRequest = {
    request
        .withQueryString(wrapper.queryParams.toSeq: _*)
        .withQueryString(wrapper.searchQueryAsParam(queryString).toSeq: _*)
  }

  /** Signs the request if the [[RestApiOAuthTrait]] is configured. */
  private def handleOAuth(wrapper: RestApiWrapperTrait,
                          request: WSRequest): WSRequest = {
    wrapper match {
      case oAuthWrapper: RestApiOAuthTrait =>
        request
            .sign(OAuthCalculator(
              oAuthWrapper.oAuthConsumerKey,
              oAuthWrapper.oAuthRequestToken))
      case _ =>
        request
    }
  }

  // Return all wrapper ids as a JSON list
  def wrapperIds() = {
    Action {
      Ok(JsArray(WrapperController.sortedWrapperIds.map(id => JsString(id))))
    }
  }
}

/**
 * For now, hard code all available wrappers here. Later this should probably be replaced by a plugin mechanism.
 */
object WrapperController {
  val wrapperMap = Map[String, RestApiWrapperTrait](
    "gplus" -> new GooglePlusWrapper(),
    "twitter" -> new TwitterWrapper(),
    "gkb" -> new GoogleKnowledgeGraphWrapper(),
    "ebay" -> new EBayWrapper()
  )

  val sortedWrapperIds = wrapperMap.keys.toSeq.sortWith(_ < _)
}