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

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import controllers.Application
import controllers.de.fuhsen.wrappers.dataintegration.{EntityLinking, SilkConfig, SilkTransformableTrait}
import controllers.de.fuhsen.wrappers.security.{RestApiOAuth2Trait, RestApiOAuthTrait}
import org.apache.jena.graph.Triple
import org.apache.jena.query.{Dataset, DatasetFactory}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.core.Quad
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, JsString}
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.{Action, Controller, Result}
import utils.dataintegration.RDFUtil._
import utils.dataintegration.{RequestMerger, UriTranslator}
import controllers.de.fuhsen.common.{ApiError, ApiResponse, ApiSuccess}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * Handles requests to API wrappers. Wrappers must at least implement [[RestApiWrapperTrait]].
  * Depending on implemented traits also does transformation, linking and merging of entities.
  */
class WrapperController @Inject()(ws: WSClient) extends Controller {
  val requestCounter = new AtomicInteger(0)

  def search(wrapperId: String, query: String) = Action.async {
    WrapperController.wrapperMap.get(wrapperId) match {
      case Some(wrapper) =>
        Logger.info(s"Starting $wrapperId Search with query: " + query)
        execQueryAgainstWrapper(query, wrapper) map {
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

  private def execQueryAgainstWrapper(query: String, wrapper: RestApiWrapperTrait): Future[ApiResponse] = {
    val apiRequest = createApiRequest(wrapper, query)

    if(!wrapper.requestType.equals("JAVA")){
      Logger.info("GET wrapper request")

      val apiResponse = executeApiRequest(apiRequest)
      val customApiResponse = customApiHandling(wrapper, apiResponse)
      transformApiResponse(wrapper, customApiResponse, null)

    }else{
      Logger.info("POST wrapper request")

      transformApiResponse(wrapper, null, apiRequest.url)
    }

  }

  /**
    * Returns the merged result from multiple wrappers in N-Quads format.
    *
    * @param query      for each wrapper
    * @param wrapperIds a comma-separated list of wrapper ids
    */
  def searchMultiple(query: String, wrapperIds: String) = Action.async {
    val wrappers = (wrapperIds.split(",") map (WrapperController.wrapperMap.get)).toSeq
    if (wrappers.exists(_.isEmpty)) {
      Future(BadRequest("Invalid wrapper requested! Supported wrappers: " +
          WrapperController.sortedWrapperIds.mkString(", ")))
    } else {
      fetchAndIntegrateWrapperResults(wrappers, query)
    }
  }

  /**
    * Returns the merged result from multiple wrappers in JSON-LD format.
    *
    * @param query for each wrapper
    * @param wrapperIds a comma-separated list of wrapper ids
    */
  def searchMultiple2(query: String, wrapperIds: String) = Action.async {
    val wrappers = (wrapperIds.split(",") map (WrapperController.wrapperMap.get)).toSeq
    if(wrappers.exists(_.isEmpty)) {
      Future(BadRequest("Invalid wrapper requested! Supported wrappers: " +
        WrapperController.sortedWrapperIds.mkString(", ")))
    } else {
      val requestMerger = new RequestMerger()
      val resultFutures = wrappers.flatten map (wrapper => execQueryAgainstWrapper(query, wrapper))
      Future.sequence(resultFutures) map { results =>
        for ((wrapperResult, wrapper) <- results.zip(wrappers.flatten)) {
          wrapperResult match {
            case ApiSuccess(responseBody) => Logger.debug("POST-SILK:"+responseBody)
              val model = rdfStringToModel(responseBody, Lang.JSONLD.getName) //Review
              requestMerger.addWrapperResult(model, wrapper.sourceUri)
            case _: ApiError =>
          }
        }
        //val resultDataset = requestMerger.constructQuadDataset()
        Ok(requestMerger.serializeMergedModel(Lang.JSONLD))
      }
    }
  }


  /**
    * Link and merge entities from different sources.
 *
    * @param wrappers
    * @param query
    * @return
    */
  private def fetchAndIntegrateWrapperResults(wrappers: Seq[Option[RestApiWrapperTrait]],
                                      query: String): Future[Result] = {
    // Fetch the transformed results from each wrapper
    val resultFutures = wrappers.flatten map (wrapper => execQueryAgainstWrapper(query, wrapper))
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
        case ApiSuccess(responseBody) =>
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
    * @param inputDataset
    * @param sameAs
    * @return
    */
  private def rewriteDatasetBasedOnSameAsLinks(inputDataset: Dataset,
                                               sameAs: Future[Option[Traversable[Triple]]]): Future[Dataset] = {
    sameAs map {
      case Some(sameAsTriples) =>
        val it = inputDataset.asDatasetGraph().find()
        val quads = ArrayBuffer.empty[Quad]
        while(it.hasNext) {
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
      quads = quads.toTraversable,
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
      case ApiSuccess(body) =>
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
          case ApiSuccess(body) =>
            customFn(body).
                map(customResult => ApiSuccess(customResult))
          case r: ApiError =>
            Future(r)
        }
      case None =>
        apiResponse
    }
  }

  /** Handles transformation if configured for the wrapper */
  private def transformApiResponse(wrapper: RestApiWrapperTrait,
                                   apiResponse: Future[ApiResponse],
                                   apiUrl:String): Future[ApiResponse] = {
    if(apiResponse != null){
      apiResponse.flatMap {
        case error: ApiError =>
          // There has been an error previously, don't go on.
          Future(error)
        case ApiSuccess(body) =>
          Logger.debug("PRE-SILK: "+body)
          handleSilkTransformation(wrapper, body)
      }
    }else{
      wrapper match {
        case oAuthWrapper: RestApiOAuthTrait =>
          val bodyJava = new Application().javaRequest(oAuthWrapper, apiUrl)
          Logger.debug("PRE-SILK (Java): " + bodyJava )
          handleSilkTransformation(wrapper, bodyJava)
      }
    }
  }

  /** Executes the request to the wrapped REST API */
  private def executeApiRequest(apiRequest: WSRequest): Future[ApiResponse] = {
    apiRequest.get.map(convertToApiResponse("Wrapper or the wrapped service"))
  }

  /** If transformations are configured then execute them via the Silk REST API */
  def handleSilkTransformation(wrapper: RestApiWrapperTrait,
                               content: String,
                               acceptType: String = "text/turtle"): Future[ApiResponse] = {
                               //acceptType: String = "text/csv"): Future[ApiResponse] = {
    wrapper match {
      case silkTransform: SilkTransformableTrait if silkTransform.silkTransformationRequestTasks.size > 0 =>
        Logger.info("Execute Silk Transformations")
        val lang = acceptTypeToRdfLang(acceptType)
        val futureResponses = executeTransformation(content, acceptType, silkTransform)
        val rdf = convertToRdf(lang, futureResponses)
        rdf.map(content => ApiSuccess(content))
      case _ =>
        // No transformation to be executed
        Future(ApiSuccess(content))
    }
  }

  /** Execute all transformation tasks on the content */
  private def executeTransformation(content: String,
                                    acceptType: String,
                                    silkTransform: RestApiWrapperTrait with SilkTransformableTrait): Seq[Future[ApiResponse]] = {
    for (transform <- silkTransform.silkTransformationRequestTasks) yield {
      Logger.info("Executing silk transformation: "+transform.transformationTaskId)
      //val task = silkTransform.silkTransformationRequestTasks.head
      val transformRequest = ws.url(silkTransform.transformationEndpoint(transform.transformationTaskId))
          .withHeaders("Content-Type" -> "application/xml; charset=utf-8")
          //.withHeaders("Accept" -> acceptType)
      val response = transformRequest
          .post(transform.silkTransformationRequestBodyGenerator(content))
          .map(convertToApiResponse("Silk transformation endpoint"))
      response
    }
  }

  private def convertToApiResponse(serviceName: String)(response: WSResponse): ApiResponse = {
    if (response.status >= 400) {
      ApiError(response.status, s"There was a problem with the $serviceName. Service response:\n\n" + response.body)
    } else if (response.status >= 300) {
      ApiError(response.status, s"$serviceName seems to be configured incorrectly, received a redirect.")
    } else {
      ApiSuccess(response.body)
    }
  }

  /**
    * Executes the person linking rule and returns a set of sameAs links.
    *
    * @param content The RDF content as String.
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
        .map(convertToApiResponse("Silk linking service"))
  }

  /** Merge all transformation results into a single model and return the serialized model */
  private def convertToRdf(lang: String,
                           futureResponses: Seq[Future[ApiResponse]]): Future[String] = {
    Future.sequence(futureResponses) map { responses =>
      val model = ModelFactory.createDefaultModel()
      responses.foreach {
        case ApiSuccess(body) =>
          model.add(rdfStringToModel(body, lang))
        case ApiError(statusCode, errorMessage) =>
          Logger.warn(s"Got status code $statusCode with message: $errorMessage")
      }
      modelToTripleString(model, "application/ld+json")
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
  val wrapperMap = Map[String, RestApiWrapperTrait](
    //Social Networks
    "gplus" -> new GooglePlusWrapper(),
    "twitter" -> new TwitterWrapper(),
    "facebook" -> new FacebookWrapper(),
    //Knowledge base
    "gkb" -> new GoogleKnowledgeGraphWrapper(),
    //eCommerce
    "ebay" -> new EBayWrapper(),
    //Darknet
    "tor2web" -> new Tor2WebWrapper(),
    //Linked leaks
    "linkedleaks" -> new LinkedLeaksWrapper(),
    //OCCRP
    "occrp" -> new OCCRPWrapper(),
    //Xing
    "xing" -> new XingWrapper(),
    //Elastic Search
    "elasticsearch" -> new ElasticSearchWrapper()
  )

  val sortedWrapperIds = wrapperMap.keys.toSeq.sortWith(_ < _)
}