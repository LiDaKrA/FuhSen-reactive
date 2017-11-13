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

/**
  * Wrapper for the VK REST API.
  */
class VkWrapper extends RestApiWrapperTrait with SilkTransformableTrait with RestApiOAuth2Trait {

  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map(
    "count" -> "100",
    "fields" -> ConfigFactory.load.getString("vk.search.fields"),
    "v" -> "5.8")

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    Map("q" -> queryString)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("vk.search.url")

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "vk"

  //RestApiOAuth2Trait implementation:
  override def oAuth2ClientKey : String =  ConfigFactory.load.getString("vk.app.key")
  override def oAuth2ClientSecret : String =  ConfigFactory.load.getString("vk.app.key")
  override def oAuth2AccessToken : String = {if (!TokenManager.getAccessTokenByProvider("vk").isEmpty) TokenManager.getAccessTokenByProvider("vk").get.access_token else ""}


  /** SILK Transformation Trait **/
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "VkPersonTransformation",
      createSilkTransformationRequestBody(
        basePath = "/response/items",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/vk/{id}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")


  override def requestType: String = "GET"
}
