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
import controllers.de.fuhsen.wrappers.security.RestApiOAuthTrait
import play.api.libs.oauth.{ConsumerKey, RequestToken}

/**
 * Wrapper for the Twitter REST API.
 */
class TwitterWrapper extends RestApiWrapperTrait with RestApiOAuthTrait with SilkTransformableTrait {
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map("count" -> "100")

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    val query_string: String = queryString.replace(" ", "+")
    Map("q" -> query_string)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("twitter.url")

  override def oAuthRequestToken: RequestToken = RequestToken(
    ConfigFactory.load.getString("twitter.access.token"),
    ConfigFactory.load.getString("twitter.access.secret"))

  override def oAuthConsumerKey: ConsumerKey = ConsumerKey(
    ConfigFactory.load.getString("twitter.consumer.key"),
    ConfigFactory.load.getString("twitter.consumer.secret"))

  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "TwitterPersonTransformation",
      createSilkTransformationRequestBody(
        basePath = "",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/twitter/{id}"
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
  override def sourceLocalName: String = "twitter"

  override def requestType: String = "JAVA"
}
