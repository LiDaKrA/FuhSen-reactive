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
import controllers.de.fuhsen.wrappers.security.{RestApiOAuth2Trait, TokenManager}
import org.apache.jena.atlas.json.JSON
import play.api.libs.json.Json


/**
  * Created by cmorales on 16.03.2016.
  */
class FacebookWrapper extends RestApiWrapperTrait with RestApiOAuth2Trait with SilkTransformableTrait with PaginatingApiTrait {

  //RestApiWrapperTrait implementation:
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map("type" -> "user",
                                                      "fields" -> ConfigFactory.load.getString("facebook.search.fields"))
  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    var query_string: String = queryString.replace(" ", "+")
    query_string = query_string.replace("%20", "+")
    Map("q" -> query_string)
  }
  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("facebook.search.url")
  override def sourceLocalName: String = "facebook"

  //RestApiOAuth2Trait implementation:
  override def oAuth2ClientKey : String =  ConfigFactory.load.getString("facebook.app.key")
  override def oAuth2ClientSecret : String =  ConfigFactory.load.getString("facebook.app.secret")
  override def oAuth2AccessToken : String = {if (!TokenManager.getAccessTokenByProvider("facebook").isEmpty) TokenManager.getAccessTokenByProvider("facebook").get.access_token else ""}

  //SilkTransformableTrait implementation:
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "FacebookPersonTransformation",
      createSilkTransformationRequestBody(
        basePath = "data",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/facebook/{id}"
      )
    )
  )
  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin
  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  /** The query parameter to specify the page/offset in the result set */
  override def nextPageQueryParameter: String = "after"

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    *
    * @param resultBody The body serialized as String as coming from the API.
    * @param lastValue  The last value. This can be used if the value is not available in the result body, but instead
    *                   is calculated by the wrapper implementation.
    */
  override def extractNextPageQueryValue(resultBody: String, lastValue: Option[String]): Option[String] = {
    val jsonBody = Json.parse(resultBody)
    (jsonBody \ "paging" \ "next").toOption map (_.as[String])
  }
}