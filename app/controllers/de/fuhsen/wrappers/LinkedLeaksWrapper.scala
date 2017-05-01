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

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map("Accept" -> "application/json")

  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {

    var final_query = s"""
                          |PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
                          |PREFIX lucene: <http://www.ontotext.com/connectors/lucene#>
                          |PREFIX leak: <http://data.ontotext.com/resource/leak/>
                          |PREFIX gn: <http://www.geonames.org/ontology#>
                          |PREFIX dbr: <http://dbpedia.org/resource/>
                          |PREFIX onto: <http://www.ontotext.com/>
                          |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                          |
                          |select ?node_id ?node ?type ?name ?countries ?address ?company_type ?company ?jurisdiction_description ?note ?original_name ?service_provider ?status ?valid_until
                          |
                          |from onto:disable-sameAs
                          |{
                          |    ?search a inst:all-nodes2;
                          |    lucene:query '''name:"$queryString"''' ;
                          |    lucene:entities ?node .
                          |    ?node a ?type .
                          |    ?node leak:name ?name .
                          |    OPTIONAL { ?node leak:address ?address } .
                          |    OPTIONAL { ?node leak:node_id ?node_id } .
                          |    OPTIONAL { ?node leak:company_type ?company_type } .
                          |    OPTIONAL { ?node leak:countries ?countries } .
                          |    OPTIONAL { ?node leak:company ?company } .
                          |    OPTIONAL { ?node leak:jurisdiction_description ?jurisdiction_description } .
                          |    OPTIONAL { ?node leak:note ?note } .
                          |    OPTIONAL { ?node leak:original_name ?original_name } .
                          |    OPTIONAL { ?node leak:service_provider ?service_provider } .
                          |    OPTIONAL { ?node leak:status ?status } .
                          |    OPTIONAL { ?node leak:valid_until ?valid_until } .
                          |    FILTER(?type IN (leak:Intermediary, leak:Entity, leak:Officer )) .
                          |}
      """.stripMargin

    Map("query" -> java.net.URLEncoder.encode(final_query, "UTF-8"))
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
      transformationTaskId = "LinkedLeaksEntityTransformation",
      createSilkTransformationRequestBody(
        basePath = "results/bindings",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/linkedleaks/{node_id/value}"
      )
    )
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

}
