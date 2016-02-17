package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory

import scala.xml.Elem

/**
 * Wrapper around EBay product search API.
 */
class EBayWrapper extends RestApiWrapperTrait with SilkTransformableTrait {
  /**
   * Query parameters that should be added to the request.
   * @return
   */
  override def queryParams: Map[String, String] = Map(
    "OPERATION-NAME" -> "findItemsByKeywords",
    "SERVICE-VERSION" -> "1.0.0",
    "SECURITY-APPNAME" -> ConfigFactory.load.getString("ebay.app.key"),
    "GLOBAL-ID" -> "EBAY-DE",
    "RESPONSE-DATA-FORMAT" -> "JSON",
    "REST-PAYLOAD" -> "",
    "paginationInput.entriesPerPage" -> "20"
  )

  /**
   * Returns for a given query string the representation as query parameter for the specific API.
   *
   * @param queryString
   * @return
   */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("keywords" -> queryString)
  }

  /**
   * The REST endpoint URL
   * @return
   */
  override def apiUrl: String = ConfigFactory.load.getString("ebay.url")

  /**
   * The type of the transformation input.
   */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /**
   * The task id of the transformation task of the project.
   */
  override def transformationTaskId: String = "eBayTransformation"

  override def silkTransformationRequestBody(content: String): Elem = {
    createSilkTransformationRequestBody(
      content = content,
      basePath = "findItemsByKeywordsResponse/searchResult/item",
      uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/ebay/{id}"
    )
  }

  /**
   * The project id of the Silk project
   */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")
}
