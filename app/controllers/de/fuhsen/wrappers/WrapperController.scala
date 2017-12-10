/*
 * Copyright (C) 2016 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers.de.fuhsen.wrappers

import java.net.ConnectException
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import controllers.Application
import controllers.de.fuhsen.FuhsenVocab
import controllers.de.fuhsen.common.{ApiError, ApiResponse, ApiSuccess, ModelBodyParser}
import controllers.de.fuhsen.wrappers.dataintegration.{EntityLinking, SilkConfig, SilkTransformableTrait}
import controllers.de.fuhsen.wrappers.security.{RestApiOAuth2Trait, RestApiOAuthTrait}
import org.apache.jena.graph.Triple
import org.apache.jena.query.{Dataset, DatasetFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.core.Quad
import play.Logger
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc._
import utils.dataintegration.RDFUtil._
import utils.dataintegration.{RDFUtil, RequestMerger, UriTranslator}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * Handles requests to API wrappers. Wrappers must at least implement [[RestApiWrapperTrait]].
  * Depending on implemented traits also does transformation, linking and merging of entities.
  */
class WrapperController @Inject()(ws: WSClient) extends Controller {
  val requestCounter = new AtomicInteger(0)

  def search(wrapperId: String, query: String) = Action.async { request =>
    WrapperController.wrapperMap.get(wrapperId) match {
      case Some(wrapper) =>
        Logger.info(s"Starting $wrapperId Search with query: " + query)
        execQueryAgainstWrapper(query, wrapper, createWrapperMetaData(request.body)) map {
          case errorResult: ApiError =>
            InternalServerError(errorResult.errorMessage + " API status code: " + errorResult.statusCode)
          case success: ApiSuccess =>
            Ok(success.responseBody)
        }
      case None =>
        Future(NotFound("Wrapper " + wrapperId + " not found! Supported wrapper: " +
          WrapperController.sortedWrapperIds.mkString(", ")))
    }
  }

  private def createWrapperMetaData(body: AnyContent): Option[Model] = {
    ModelBodyParser.parse(body)
    //ToDo: Validate that the provided model is PROV data.
  }

  private def execQueryAgainstWrapper(query: String,
                                          wrapper: RestApiWrapperTrait,
                                          searchMetaData: Option[Model]): Future[ApiResponse] = {
    val apiRequest = createApiRequest(wrapper, query, searchMetaData)
    val apiResponse = executeApiRequest(wrapper, apiRequest)
    val customApiResponse = customApiHandling(wrapper, apiResponse)
    val transformedApiResponse = transformApiResponse(wrapper, customApiResponse)
    addProvMetaData(wrapper, transformedApiResponse)
  }

  /** Creates the complete API REST request */
  def createApiRequest(wrapper: RestApiWrapperTrait,
                       query: String,
                       searchMetaData: Option[Model]): WSRequest = {
    val apiRequestWithApiParams: WSRequest = getWrapperApiUrl(wrapper, query, searchMetaData)
    //apiRequestWithApiParams.withRequestTimeout(ConfigFactory.load.getLong("fuhsen.wrapper.request.timeout"))
    //Logger.info(s"${wrapper.sourceLocalName} timeout set to ${ConfigFactory.load.getLong("fuhsen.wrapper.request.timeout")}")
    val apiRequestWithOAuthIfNeeded = handleOAuth(wrapper, apiRequestWithApiParams)
    apiRequestWithOAuthIfNeeded
  }

  private def getWrapperApiUrl(wrapper: RestApiWrapperTrait,
                               query: String,
                               searchMetaData: Option[Model]): WSRequest = {

    searchMetaData match {
      case Some(model) =>
        Logger.info("Loading next page results URL")
        val nextPageResults = FuhsenVocab.getProvAgentNextPage(model, wrapper.sourceLocalName)
        if (nextPageResults.isEmpty)
          addQueryParameters(wrapper, query)
        else {
          Logger.info("Next Page URL loaded successfully: "+nextPageResults)
          ws.url(nextPageResults)
        }
      case None =>
        addQueryParameters(wrapper, query)
    }
  }

  /** Add all query parameters to the request. */
  private def addQueryParameters(wrapper: RestApiWrapperTrait,
                                 queryString: String): WSRequest = {

    var url_with_params = wrapper.apiUrl+"?"

    for ((k,v) <- wrapper.searchQueryAsParam(queryString)){
      url_with_params = url_with_params.concat(k+"="+v+"&")
    }

    for ((k,v) <- wrapper.queryParams){
      url_with_params = url_with_params.concat(k+"="+v+"&")
    }

    val apiRequest: WSRequest = ws.url(url_with_params.dropRight(1))

    apiRequest.withHeaders(wrapper.headersParams.toSeq: _*)
  }

  /** Executes the complete API REST request asynchronously. */
  private def executeApiRequest(wrapper: RestApiWrapperTrait,
                                        request: WSRequest) : Future[ApiResponse] = {

    if(!wrapper.requestType.equals("JAVA"))
      request.withRequestTimeout(ConfigFactory.load.getLong("fuhsen.wrapper.request.timeout"))
          .get.map(convertToApiResponse("Wrapper or the wrapped service", Some(wrapper), Some(request.url))) //Add here the original API URI
        .recover {
        case e: ConnectException =>
          Logger.warn("Connection Exception during wrapper execution: "+e.getMessage)
          ApiError(INTERNAL_SERVER_ERROR, e.getMessage)
        case e: Exception =>
          Logger.warn(s"Generic Exception during wrapper execution ${e.getMessage}")
          ApiError(INTERNAL_SERVER_ERROR, e.getMessage)
      }
    else {
      wrapper match {
        case oAuthWrapper: RestApiOAuthTrait =>
          Logger.info("Using JAVA impl to call the rest api")
          val bodyJava = new Application().javaRequest(oAuthWrapper, request.url)
          if (bodyJava.startsWith("NOT OK")) {
            val erroDetails = bodyJava.split("-")
            Future(ApiError(erroDetails(1).toInt,erroDetails(2)))
          }
          else
            Future(ApiSuccess(bodyJava))
        case _ => Future(ApiError(INTERNAL_SERVER_ERROR, "Not correct wrapper definition"))
      }
    }
  }

  /** Handles transformation if configured for the wrapper */
  private def transformApiResponse(wrapper: RestApiWrapperTrait,
                                   apiResponse: Future[ApiResponse]): Future[ApiResponse] = {

    apiResponse.flatMap {
        case error: ApiError =>
          // There has been an error previously, don't go on.
          Future(error)
        case ApiSuccess(body, nextPage, lastValue) =>
          //Logger.info("PRE-SILK: "+body)
          handleSilkTransformation(wrapper, body, nextPage)
      }
  }

  private def addProvMetaData(wrapper: RestApiWrapperTrait, apiResponse: Future[ApiResponse]) :  Future[ApiSuccess] = {
    apiResponse map {
      case errorResult: ApiError => {
        val model = FuhsenVocab.createProvModel(wrapper.sourceLocalName,errorResult.statusCode.toString, errorResult.statusCode.toString, None, None)
        ApiSuccess(RDFUtil.modelToTripleString(model, Lang.JSONLD))
      }
      case success: ApiSuccess => {
        //ToDo: Review and remove unnecessary transformation from model to string and string to model
        val model = RDFUtil.rdfStringToModel(success.responseBody, Lang.JSONLD)
        model.add(FuhsenVocab.createProvModel(wrapper.sourceLocalName,"200","OK", success.nextPage, success.lastValue)) //Adding PROV metadata
        ApiSuccess(RDFUtil.modelToTripleString(model, Lang.JSONLD))
      }
    }
  }

  def searchMultiple(query: String, wrapperIds: String) = Action.async { request =>
    Logger.info("Starting search multiple: "+query+" "+wrapperIds)
    val wrappers = (wrapperIds.split(",") map WrapperController.wrapperMap.get).toSeq
    if (wrappers.exists(_.isEmpty)) {
      Future(BadRequest("Invalid wrapper requested! Supported wrappers: " +
        WrapperController.sortedWrapperIds.mkString(", ")))
    } else {
      val requestMerger = new RequestMerger()
      val resultFutures = wrappers.flatten map (wrapper => execQueryAgainstWrapper(query, wrapper, createWrapperMetaData(request.body)))
      Future.sequence(resultFutures) map { results =>
        for ((wrapperResult, wrapper) <- results.zip(wrappers.flatten)) {
          wrapperResult match {
            case ApiSuccess(responseBody, nextPage, lastValue) =>
              //Logger.debug("POST-SILK:" + responseBody)
              val model = rdfStringToModel(responseBody, Lang.JSONLD.getName) //Review
              requestMerger.addWrapperResult(model, wrapper.sourceUri)
            case e: ApiError =>
              Logger.error(s"Error code ${e.statusCode} in response of wrapper " + wrapper.sourceLocalName + ": " + e.errorMessage)
          }
        }
        Ok(requestMerger.serializeMergedModel(Lang.JSONLD))
      }
    }
  }

  //-----------------------------------------------------------

  private implicit val validationErrorsWrites: Writes[ValidationError] = new Writes[ValidationError] {
    override def writes(o: ValidationError): JsValue = {
      Json.obj(
        "messages" -> JsArray(o.messages.map(Json.toJson(_))),
        "args" -> JsArray(o.args.map(_.toString).map(Json.toJson(_)))
      )
    }
  }

  private def extractNextPageMetaData(wrapper: RestApiWrapperTrait,
                                      responseBody: String,
                                      lastApiCallUrl: Option[String]): Option[String] = {
    Logger.info("Executing extractNextPageMetaData "+wrapper.sourceLocalName)
    wrapper match {
      case paginatingApi: PaginatingApiTrait =>
        Logger.info("It is a PaginatingApiTrait "+wrapper.sourceLocalName)
        paginatingApi.extractNextPageQueryValue(responseBody, lastApiCallUrl)
      case _ => // Wrapper does not support pagination, do nothing
        None
    }
  }

  /**
    * Link and merge entities from different sources.
    *
    * @param wrappers The wrapper implementations
    * @param query    the search query
    * @return
    */
  private def fetchAndIntegrateWrapperResults(wrappers: Seq[Option[RestApiWrapperTrait]],
                                              query: String): Future[Result] = {
    // Fetch the transformed results from each wrapper
    val resultFutures = wrappers.flatten map (wrapper => execQueryAgainstWrapper(query, wrapper, null))
    Future.sequence(resultFutures) flatMap { results =>
      // Merge results
      val requestMerger = mergeWrapperResults(wrappers, results)
      // Link entities
      val sameAsTriples = personLinking(requestMerger.serializeMergedModel(Lang.TURTLE), langToAcceptType(Lang.TURTLE))
      val resultDataset = requestMerger.constructQuadDataset()
      // Rewrite/merge entities based on entity linking
      val rewrittenDataset = rewriteDatasetBasedOnSameAsLinks(resultDataset, sameAsTriples)
      datasetToNQuadsResult(rewrittenDataset)
    }
  }

  private def datasetToNQuadsResult(rewrittenDataset: Future[Dataset]): Future[Result] = {
    rewrittenDataset map { d =>
      Ok(datasetToQuadString(d, Lang.JSONLD)).
          withHeaders(("content-type", Lang.JSONLD.getContentType.getContentType))
    }
  }

  private def mergeWrapperResults(wrappers: Seq[Option[RestApiWrapperTrait]],
                                  results: Seq[ApiResponse]): RequestMerger = {
    val requestMerger = new RequestMerger()
    for ((wrapperResult, wrapper) <- results.zip(wrappers.flatten)) {
      wrapperResult match {
        case ApiSuccess(responseBody, nextPage, lastValue) =>
          val model = rdfStringToModel(responseBody, Lang.JSONLD.getName)
          requestMerger.addWrapperResult(model, wrapper.sourceUri)
        case _: ApiError =>
        // Ignore for now
      }
    }
    requestMerger
  }

  /**
    * Rewrites the entity URIs based on the sameAs links. All entities of each transitive closure will
    * have the same URI and point to the original URI via sameAs link (one per rewritten entity and source graph).
    *
    * @param inputDataset input Jena Dataset
    * @param sameAs       sameAs links used to rewrite URIs in the dataset
    * @return
    */
  private def rewriteDatasetBasedOnSameAsLinks(inputDataset: Dataset,
                                               sameAs: Future[Option[Traversable[Triple]]]): Future[Dataset] = {
    sameAs map {
      case Some(sameAsTriples) =>
      val it = inputDataset.asDatasetGraph().find()
      val quads = ArrayBuffer.empty[Quad]
      while (it.hasNext) {
        quads.append(it.next())
      }
      rewriteDatasetBasedOnSameAsLinks(sameAsTriples, quads)
      case None =>
        inputDataset
    }
  }

  private def rewriteDatasetBasedOnSameAsLinks(sameAsTriples: Traversable[Triple],
                                               quads: ArrayBuffer[Quad]): Dataset = {
    val translatedQuads = UriTranslator.translateQuads(
      quads = quads,
      links = sameAsTriples
    )
    val translatedDataset = DatasetFactory.create()
    val datasetGraph = translatedDataset.asDatasetGraph()
    for (quad <- translatedQuads) {
      datasetGraph.add(quad)
    }
    translatedDataset
  }

  def personLinking(entityRDF: String, acceptType: String): Future[Option[Traversable[Triple]]] = {
    executePersonLinking(entityRDF, acceptType) map {
      case ApiSuccess(body, nextPage, lastValue) =>
        Some(stringToTriple(body, acceptTypeToRdfLang(acceptType)))
      case ApiError(status, message) =>
        Logger.warn(s"Person linking service returned a status code of $status")
        None
    }
  }

  /** If a custom response handling is defined execute it against the response if has not been an error */
  private def customApiHandling(wrapper: RestApiWrapperTrait,
                                apiResponse: Future[ApiResponse]): Future[ApiResponse] = {
    wrapper.customResponseHandling(ws) match {
      case Some(customFn) =>
        apiResponse.flatMap {
          case ApiSuccess(body, nextPage, lastValue) =>
            Logger.info("Custom Api Handling: Api Success")
            customFn(body).
                map(customResult => ApiSuccess(customResult, nextPage, lastValue))
          case r: ApiError =>
            Logger.info("Custom Api Handling: Api Error")
            Future(r)
        }
      case None =>
        apiResponse
    }
  }


  /** If transformations are configured then execute them via the Silk REST API */
  def handleSilkTransformation(wrapper: RestApiWrapperTrait,
                               content: String,
                               nextPage: Option[String],
                               acceptType: String = "text/turtle"): Future[ApiResponse] = {
    wrapper match {
      case silkTransform: SilkTransformableTrait if silkTransform.silkTransformationRequestTasks.nonEmpty =>
        Logger.info("Execute Silk Transformations")
        val lang = acceptTypeToRdfLang(acceptType)
        try {
          val futureResponses = executeTransformation(content, acceptType, silkTransform)
          val rdf = convertToRdf(lang, futureResponses)
          rdf.map(content => ApiSuccess(content, nextPage))
        } catch {
          //Handling error in SILK Transformation task
          case e: Exception =>
            Logger.error("Error during SILK Transformation task: "+e.getMessage)
            Future(ApiError(500,e.getMessage))
        }
      case _ =>
        Logger.info("No transformation to be executed")
        Future(ApiSuccess(content, nextPage))
    }
  }

  /** Execute all transformation tasks on the content */
  private def executeTransformation(content: String,
                                    acceptType: String,
                                    silkTransform: RestApiWrapperTrait with SilkTransformableTrait): Seq[Future[ApiResponse]] = {
    for (transform <- silkTransform.silkTransformationRequestTasks) yield {
      Logger.info("Executing silk transformation: " + transform.transformationTaskId)
      //val task = silkTransform.silkTransformationRequestTasks.head
      val transformRequest = ws.url(silkTransform.transformationEndpoint(transform.transformationTaskId))
          .withHeaders("Content-Type" -> "application/xml; charset=utf-8")
      //.withHeaders("Accept" -> acceptType)
      val response = transformRequest
          .post(transform.silkTransformationRequestBodyGenerator(content))
          .map(convertToApiResponse("Silk transformation endpoint", None, None))
      response
    }
  }

  private def convertToApiResponse(serviceName: String,
                                   wrapper: Option[RestApiWrapperTrait],
                                   lastApiCallUrl: Option[String])(response: WSResponse): ApiResponse = {
    Logger.info("Reponse Status: "+response.status)
    if (response.status >= 400) {
      ApiError(response.status, s"There was a problem with the $serviceName. Service response:\n\n" + response.body)
    } else if (response.status >= 300) {
      ApiError(response.status, s"$serviceName seems to be configured incorrectly, received a redirect.")
    } else {
      wrapper match {
        case Some(wrapperImpl) =>
          val nextPage = extractNextPageMetaData(wrapperImpl, response.body, lastApiCallUrl)
          Logger.info("Next Page: "+nextPage)
          ApiSuccess(response.body, nextPage)
        case _ => ApiSuccess(response.body)
      }
    }
  }

  /**
    * Executes the person linking rule and returns a set of sameAs links.
    *
    * @param content    The RDF content as String.
    * @param acceptType An HTTP accept type that is used for serialization and deserialization from/to the REST
    *                   services.
    * @return
    */
  private def executePersonLinking(content: String,
                                   acceptType: String): Future[ApiResponse] = {
    val silkConfig = SilkConfig(
      projectId = ConfigFactory.load.getString("silk.socialApiProject.id"),
      linkingTaskId = ConfigFactory.load.getString("silk.linking.task.person"),
      silkServerUrl = ConfigFactory.load.getString("silk.server.url")
    )
    val entityLinking = new EntityLinking(silkConfig)
    val linkRequest = ws.url(silkConfig.endpoint)
        .withHeaders("Content-Type" -> "application/xml")
        .withHeaders("Accept" -> acceptType)
    linkRequest.post(entityLinking.linkTemplate(content, acceptTypeToRdfLang(acceptType)))
        .map(convertToApiResponse("Silk linking service", None, None))
  }

  /** Merge all transformation results into a single model and return the serialized model */
  private def convertToRdf(lang: String,
                           futureResponses: Seq[Future[ApiResponse]]): Future[String] = {
    Future.sequence(futureResponses) map { responses =>
      val model = ModelFactory.createDefaultModel()
      responses.foreach {
        case ApiSuccess(body, nextPage, lastValue) =>
          model.add(rdfStringToModel(body, lang))
        case ApiError(statusCode, errorMessage) =>
          Logger.warn(s"Got status code $statusCode with message: $errorMessage")
      }
      modelToTripleString(model, "application/ld+json")
    }
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

      case oAuth2Wrapper: RestApiOAuth2Trait =>
        request.withQueryString("access_token" -> oAuth2Wrapper.oAuth2AccessToken)
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
  val wrappers = Seq(
    new GooglePlusWrapper(),
    new TwitterWrapper(),
    new FacebookWrapper(),
    //Knowledge base
    new GoogleKnowledgeGraphWrapper(),
    //eCommerce
    new EBayWrapper(),
    //Darknet
    new Tor2WebWrapper(),
    //Linked leaks
    new LinkedLeaksWrapper(),
    //OCCRP
    new OCCRPWrapper(),
    //Xing
    new XingWrapper(),
    //Elastic Search
    new ElasticSearchWrapper(),
    //pipl
    new PiplWrapper(),
    //vk
    new VkWrapper(),
    //darknetmarkets
    new DarknetMarketsWrapper(),
    //Google Custom Search
    new GoogleCustomSearchWrapper()
  )
  val wrapperMap: Map[String, RestApiWrapperTrait] = wrappers.map { wrapper =>
    (wrapper.sourceLocalName, wrapper)
  }.toMap

  val sortedWrapperIds = wrapperMap.keys.toSeq.sortWith(_ < _)
}