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

/**
 * Wrapper around EBay product search API.
 */
class EBayWrapper extends RestApiWrapperTrait with SilkTransformableTrait {
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map(
    "OPERATION-NAME" -> "findItemsByKeywords",
    "SERVICE-VERSION" -> "1.0.0",
    "SECURITY-APPNAME" -> ConfigFactory.load.getString("ebay.app.key"),
    "GLOBAL-ID" -> "EBAY-DE",
    "RESPONSE-DATA-FORMAT" -> "JSON",
    "REST-PAYLOAD" -> "",
    "paginationInput.entriesPerPage" -> "100"
  )

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("keywords" -> queryString)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("ebay.url")

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "eBayTransformation",
      createSilkTransformationRequestBody(
        basePath = "findItemsByKeywordsResponse/searchResult/item",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/ebay/{id}"
      )
    )
  )

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  /**
    * Returns the application wide unique local name of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "ebay"
}
