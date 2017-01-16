package controllers.de.fuhsen.wrappers
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
import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.wrappers.dataintegration.{SilkTransformableTrait, SilkTransformationTask}
import controllers.de.fuhsen.wrappers.security.{RestApiOAuthTrait, TokenManager}
import play.api.libs.oauth.{ConsumerKey, RequestToken}

/**
  * Wrapper for the XING REST API.
  */
class XingWrapper extends RestApiWrapperTrait with RestApiOAuthTrait with SilkTransformableTrait {
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map(
    "limit" -> "10",
    "user_fields" -> ConfigFactory.load.getString("xing.search.fields")
  )

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    //val query_string: String = queryString.replace(" ", "%20")
    val query_string: String = queryString.split(" ").last
    Map("keywords" -> query_string)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("xing.search.url")

  override def oAuthConsumerKey: ConsumerKey = ConsumerKey(
    ConfigFactory.load.getString("xing.app.key"),
    ConfigFactory.load.getString("xing.app.secret"))

  override def oAuthRequestToken: RequestToken = RequestToken(
    TokenManager.getAccessTokenByProvider("xing").get.access_token,
    TokenManager.getAccessTokenByProvider("xing").get.token_type)

  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "XingPersonTransformation",
      createSilkTransformationRequestBody(
        basePath = "users/items/user",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/xing/{id}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "xing"

  //override def requestType: String = "JAVA"
}
