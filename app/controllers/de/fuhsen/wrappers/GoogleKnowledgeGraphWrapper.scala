package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory

import scala.xml.Elem

/**
 * Wrapper around Google Knowledge Graph API for person search.
 */
class GoogleKnowledgeGraphWrapper extends RestApiWrapperTrait with SilkTransformableTrait {
  /**
   * Query parameters that should be added to the request.
   * @return
   */
  override def queryParams: Map[String, String] = Map(
      ("key" -> ConfigFactory.load.getString("gkb.app.key")),
      ("types" -> "Person")
  )

  /**
   * Returns for a given query string the representation as query parameter for the specific API.
   *
   * @param queryString
   * @return
   */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("query" -> queryString)
  }

  /**
   * The REST endpoint URL
   * @return
   */
  override def apiUrl: String = ConfigFactory.load.getString("gkb.url")

  /**
   * The type of the transformation input.
   */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /**
   * The task id of the transformation task of the project.
   */
  override def transformationTaskId: String = "GkbResultsTransformation"

  override def silkTransformationRequestBody(content: String): Elem = {
    createSilkTransformationRequestBody(
      content = content,
      basePath = "itemListElement",
      uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/gkb/{id}"
    )
  }

  /**
   * The project id of the Silk project
   */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")
}
