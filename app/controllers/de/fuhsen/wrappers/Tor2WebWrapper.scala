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
  * Wrapper for the Tor2Web REST API.
  */
class Tor2WebWrapper extends RestApiWrapperTrait with SilkTransformableTrait with PaginatingApiTrait {
  // The number of results in one response
  final val limit = 100

  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map(
    "key" -> ConfigFactory.load.getString("tor2web.app.key"),
    "start" -> "0"
  )

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("q" -> queryString)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("tor2web.url")

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "tor2web"

  /** SILK Transformation Trait **/
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "DarkWebSitesTransformation",
      createSilkTransformationRequestBody(
        basePath = "doc",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/tor2web/{@attributes/docId}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  override def nextPageQueryParameter: String = "start"

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    *
    * @param resultBody The body serialized as String as coming from the API.
    * @param apiUrl  The last value. This can be used if the value is not available in the result body, but instead
    *                   is calculated by the wrapper implementation.
    */
  override def extractNextPageQueryValue(resultBody: String, apiUrl: Option[String]): Option[String] = {
    //Logger.info(s"Google+ extractNextPageQueryValue apiUrl: $apiUrl body: $resultBody ")
    apiUrl match {
      case Some(value) =>
        Logger.info("Calculating next page for url: "+value)
        try {
          val jsonBody = Json.parse(resultBody)

          val totalPagesString = (jsonBody \ "@attributes" \ "numFound").as[String]
          val totalPages = totalPagesString.toInt
          val lastValueString = (jsonBody \ "@attributes" \ "start").as[String]
          val lastValue = lastValueString.toInt

          Logger.info(s"Total number of results: $totalPagesString and last value: $lastValueString")

          if(lastValue > 1000)
            None
          else {
            if (lastValue+100 <= totalPages)
              Some(apiUrl.get.replace("start="+lastValue, "start="+(lastValue+100)))
            else
              None
          }
        } catch { case e: Exception =>
          Logger.warn("Error while calculating next page for Darknet: "+e)
          None
        }
      case None =>
        Logger.info("No last call URL sent, no next page created")
        None
    }
  }

}
