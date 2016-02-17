package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.xml.Elem

/**
 * Created by andreas on 2/15/16.
 */
class GooglePlusWrapper extends RestApiWrapperTrait with SilkTransformableTrait {
  override def apiUrl: String = ConfigFactory.load.getString("gplus.user.url")

  override def queryParams: Map[String, String] = Map(
    "key" -> ConfigFactory.load.getString("gplus.app.key")
  )

  /**
   * Returns for a given query string the representation as query parameter.
   * @param queryString
   * @return
   */
  override def searchQueryAsParam(queryString: String): Map[String, String] = Map(
    "query" -> queryString
  )

  /**
   * The type of the transformation input.
   */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /**
   * The task id of the transformation task of the project.
   */
  override def transformationTaskId: String = ConfigFactory.load.getString("silk.transformation.gplus.id")

  override def silkTransformationRequestBody(content: String): Elem = {
    createSilkTransformationRequestBody(
      content = content,
      basePath = "items",
      uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/gplus/{id}"
    )
  }

  /**
   * The project id of the Silk project
   */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")
}
