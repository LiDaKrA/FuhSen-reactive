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

import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.wrappers.dataintegration.{SilkTransformableTrait, SilkTransformationTask}
import play.Logger
import play.api.libs.json.Json

/**
  * Wrapper for the Linked Leaks REST API.
  */
class OCCRPWrapper extends RestApiWrapperTrait with SilkTransformableTrait with PaginatingApiTrait {

  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map("limit" -> "100")

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map("Accept" -> "application/json",
    "key" -> ConfigFactory.load.getString("occrp.search.key"))

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("q" -> queryString)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("occrp.search.url")

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "occrp"

  /** SILK Transformation Trait **/
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "OCCRPResultsTransformation",
      createSilkTransformationRequestBody(
        basePath = "results",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/occrp/{id}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    *
    * @param resultBody The body serialized as String as coming from the API.
    * @param apiUrl  The last value. This can be used if the value is not available in the result body, but instead
    *                   is calculated by the wrapper implementation.
    */
  override def extractNextPageQueryValue(resultBody: String, apiUrl: Option[String]): Option[String] = {
    try {
      val jsonBody = Json.parse(resultBody)
      (jsonBody \ "next").toOption map (_.as[String])
    } catch {
      case e: Exception =>
        Logger.error("Exception during next OCCRP page result calculation: "+e.getMessage)
        None
    }
  }

}
