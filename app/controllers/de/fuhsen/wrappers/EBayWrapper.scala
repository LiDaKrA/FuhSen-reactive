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
 * Wrapper around EBay product search API.
 */
class EBayWrapper extends RestApiWrapperTrait with SilkTransformableTrait with PaginatingApiTrait {
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map(
    "OPERATION-NAME" -> "findItemsByKeywords",
    "SERVICE-VERSION" -> "1.0.0",
    "SECURITY-APPNAME" -> ConfigFactory.load.getString("ebay.app.key"),
    "GLOBAL-ID" -> "EBAY-DE",
    "RESPONSE-DATA-FORMAT" -> "JSON",
    "REST-PAYLOAD" -> "",
    "paginationInput.entriesPerPage" -> "100",
    "paginationInput.pageNumber" -> "1"
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

  /** The query parameter to specify the page/offset in the result set */
  override def nextPageQueryParameter: String = "paginationInput.pageNumber"

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    *
    * @param resultBody The body serialized as String as coming from the API.
    * @param apiUrl  The last value. This can be used if the value is not available in the result body, but instead
    *                   is calculated by the wrapper implementation.
    */
  override def extractNextPageQueryValue(resultBody: String, apiUrl: Option[String]): Option[String] = {
    apiUrl match {
      case Some(value) =>
        Logger.info("Calculating next page for url: "+value)
        //Logger.info("resultBody: "+resultBody)
        val jsonBody = Json.parse(resultBody)

        val totalPages = ((((((jsonBody \ "findItemsByKeywordsResponse")(0)) \ "paginationOutput")(0)) \ "totalPages")(0)).asOpt[String]

        totalPages match {
          case Some(totalPagesValue) =>
            val totalPagesIntValue = totalPagesValue.toInt
            val lastValue = getParameters(value)("paginationInput.pageNumber").toInt
            Logger.info(s"totalPages $totalPagesIntValue lastValue $lastValue")
            if (lastValue+1 <= totalPagesIntValue)
              Some(apiUrl.get.replace("paginationInput.pageNumber="+lastValue, "paginationInput.pageNumber="+(lastValue+1)))
            else
              None
          case None => None
        }
      case None =>
        Logger.info("No last call URL sent, no next page created")
        None
    }
  }

  private def getParameters(url: String) : Map[String,String] = {
    val url2 = url.split("\\?").last
    url2.split("&") map { t =>
      val idx = t.indexOf("=")
      t.substring(0, idx) -> t.substring(idx + 1) } toMap
  }

}
