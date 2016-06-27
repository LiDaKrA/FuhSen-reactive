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
  * Wrapper for the Linked Leaks REST API.
  */
class LinkedLeaksWrapper extends RestApiWrapperTrait with SilkTransformableTrait {

  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    var sparql_query_template_init = "PREFIX leak: <http://data.ontotext.com/resource/leak/> PREFIX onto: <http://www.ontotext.com/> select * FROM onto:disable-sameAs { ?s leak:name "
    var sparql_query_template_end = " . } limit 100"
    var final_query = sparql_query_template_init + "\"" + queryString + "\""+ sparql_query_template_end

    Map("query" -> final_query)
  }

  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("linkedleaks.sparql.endpoint.url")

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "linkedleaks"

  /** SILK Transformation Trait **/
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "LinkedLeaksTransformation",
      createSilkTransformationRequestBody(
        basePath = "doc",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/linkedleaks/{@attributes/docId}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

}
