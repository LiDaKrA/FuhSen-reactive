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

import controllers.de.fuhsen.FuhsenVocab
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future

/**
  * Created by andreas on 2/15/16.
 */
trait RestApiWrapperTrait {
  /**
   * Query parameters that should be added to the request.
   */
  def queryParams: Map[String, String]

  /**
    * Returns the ist of headers pairs (key,value) for the specific API (if needed).
    */
  def headersParams: Map[String, String]

  /**
   * The REST endpoint URL
   */
  def apiUrl: String

  /**
   * Returns for a given query string the representation as query parameter for the specific API.
   * 
   * @param queryString
   */
  def searchQueryAsParam(queryString: String): Map[String, String]

  /**
    * Returns a custom function to transform the REST API response ibody nto a form
    * that should be processed by follow up steps. This could be used for example to
    * fetch, transform and aggregate data from additional sources and merge
    * them to a complex API response.
    */
  def customResponseHandling(implicit ws: WSClient): Option[String => Future[String]] = None

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  def sourceLocalName: String

  def sourceUri = FuhsenVocab.sourceNS + sourceLocalName
}