package controllers.de.fuhsen.wrappers

import java.io.{ByteArrayInputStream, StringWriter}
import javax.inject.Inject

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{Lang, RDFLanguages}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, JsString}
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSRequest}
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
        val customApiResponse = customApiHandling(wrapper, apiResponse)
        transformApiResponse(wrapper, customApiResponse)
      case None =>
        Future(NotFound("Wrapper " + wrapperId + " not found! Supported wrapper: " +
            WrapperController.wrapperMap.keys.mkString(", ")))
    }
  }

  /** If a custom response handling is defined execute it against the response if has not been an error */
  private def customApiHandling(wrapper: RestApiWrapperTrait,
                                apiResponse: Future[Either[ApiSuccess, ApiError]]): Future[Either[ApiSuccess, ApiError]] = {
    wrapper.customResponseHandling(ws) match {
      case Some(customFn) =>
        apiResponse.flatMap {
          case Left(result) =>
            customFn(result.responseBody).
                map(customResult => Left[ApiSuccess, ApiError](ApiSuccess(customResult)))
          case r @ Right(_) =>
            Future(r)
        }
      case None =>
        apiResponse
    }
  }

  /** Handles transformation if configured for the wrapper */
  private def transformApiResponse(wrapper: RestApiWrapperTrait,
                                   apiResponse: Future[Either[ApiSuccess, ApiError]]): Future[Result] = {
    apiResponse.flatMap {
      case Right(errorResult) =>
        // There has been an error previously, don't go on.
        Future(InternalServerError(errorResult.errorMessage + " API status code: " + errorResult.statusCode))
      case Left(success) =>
        handleSilkTransformation(wrapper, success.responseBody)
    }
  }

  /** Executes the request to the wrapped REST API */
  private def executeApiRequest(apiRequest: WSRequest): Future[Either[ApiSuccess, ApiError]] = {
    apiRequest.get.map { resp =>
      if (resp.status >= 400) {
        Right(ApiError(resp.status, "There was a problem with the wrapper or the wrapped service. Service response:\n\n" + resp.body))
      } else if (resp.status >= 300) {
        Right(ApiError(resp.status, "Wrapper seems to be configured incorrectly, received a redirect from wrapped service."))
      } else {
        Left(ApiSuccess(resp.body))
      }
    }
  }

  /** If transformations are configured then execute them via the Silk REST API */
  def handleSilkTransformation(wrapper: RestApiWrapperTrait,
                               content: String,
                               acceptType: String = "application/ld+json"): Future[Result] = {
    wrapper match {
      case silkTransform: SilkTransformableTrait =>
        Logger.info("Execute Silk Transformations")
        val lang = Option(RDFLanguages.contentTypeToLang(acceptType)).
            getOrElse(Lang.JSONLD).
            getName
        val futureResponses = for(transform <- silkTransform.silkTransformationRequestTasks) yield {
          val task = silkTransform.silkTransformationRequestTasks.head
          val transformRequest = ws.url(silkTransform.transformationEndpoint(task.transformationTaskId))
              .withHeaders("Content-Type" -> "application/xml")
              .withHeaders("Accept" -> acceptType)
          val response = transformRequest.post(task.silkTransformationRequestBodyGenerator(content)) map { response =>
            if (response.status >= 400) {
              ApiError(response.status, "There was a problem with the wrapper or the Silk transformation endpoint. Service response:\n\n" + response.body)
            } else if (response.status >= 300) {
              ApiError(response.status, "Wrapper seems to be configured incorrectly, received a redirect from Silk transformation endpoint.")
            } else {
              ApiSuccess(response.body)
            }
          }
          response
        }
        Future.sequence(futureResponses) map { responses =>
          val model = ModelFactory.createDefaultModel()
          responses.foreach {
            case ApiSuccess(body) =>
              model.add(ModelFactory.createDefaultModel().read(new ByteArrayInputStream(body.getBytes()), null, lang))
            case ApiError(statusCode, errorMessage) =>
              Logger.warn(s"Got status code $statusCode with message: $errorMessage")
          }
          val output = new StringWriter()
          model.write(output, lang)
          Ok(output.toString())
        }
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

sealed trait ApiResponse

case class ApiError(statusCode: Int, errorMessage: String) extends ApiResponse

case class ApiSuccess(responseBody: String) extends ApiResponse