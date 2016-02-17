package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory
import play.api.libs.oauth.{RequestToken, ConsumerKey}

import scala.xml.Elem

/**
 * Wrapper for the Twitter REST API.
 */
class TwitterWrapper extends RestApiWrapperTrait with RestApiOAuthTrait with SilkTransformableTrait {
  /**
   * Query parameters that should be added to the request.
   * @return
   */
  override def queryParams: Map[String, String] = Map()

  /**
   * Returns for a given query string the representation as query parameter for the specific API.
   *
   * @param queryString
   * @return
   */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    val query_string: String = queryString.replace(" ", "%20")
    Map("q" -> query_string)
  }

  /**
   * The REST endpoint URL
   * @return
   */
  override def apiUrl: String = ConfigFactory.load.getString("twitter.url")

  override def oAuthRequestToken: RequestToken = RequestToken(
    ConfigFactory.load.getString("twitter.access.token"),
    ConfigFactory.load.getString("twitter.access.secret"))

  override def oAuthConsumerKey: ConsumerKey = ConsumerKey(
    ConfigFactory.load.getString("twitter.consumer.key"),
    ConfigFactory.load.getString("twitter.consumer.secret"))

  override def silkTransformationRequestBody(content: String): Elem = {
    createSilkTransformationRequestBody(
      content = content,
      basePath = "",
      uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/twitter/{id}"
    )
  }

  /**
   * The type of the transformation input.
   */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /**
   * The task id of the transformation task of the project.
   */
  override def transformationTaskId: String = "TwitterPersonTransformation"

  /**
   * The project id of the Silk project
   */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")
}
