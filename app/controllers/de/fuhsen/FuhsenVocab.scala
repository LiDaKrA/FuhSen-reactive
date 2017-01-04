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
package controllers.de.fuhsen

import java.util.Optional

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model

/**
  * Created by andreas on 2/26/16.
  */
object FuhsenVocab {

  val NS = "http://vocab.lidakra.de/fuhsen"

  val sourceUri = NS + "#" + "merge/sourceUri"

  val sourceNS = NS + "#" + "source/"

  val url = NS + "#" + "url"

  val provenanceGraphUri = NS + "#" + "provenanceGraph"

  //************** Search Object **********************

  val SEARCH_URI = NS + "/" + "search#"
  val SEARCH = NS + "#" + "Search"
  val UID = NS + "#" + "uid"
  val QUERY_DATE = NS + "#" + "queryDate"
  val KEYWORD = NS + "#" + "keyword"
  val RANK = NS + "#" + "rank"
  val FACET_URI = NS + "/" + "facet#"
  val FACET_VAL_URI = NS + "/" + "facetVal#"
  val HAS_FACET_VAL = NS + "/" + "hasFacet"
  val FACET = NS + "#" + "Facet"
  val FACET_VALUE = NS + "#" + "value"
  val FACET_COUNT = NS + "#" + "count"
  val FACET_LABEL = NS + "#" + "facetLabel"
  val FACET_NAME = NS + "#" + "facetName"
  val DATA_SOURCE = NS + "#" + "dataSource"
  val ENTITY_TYPE = NS + "#" + "entityType"

  //SPARQL to get the keyword in a search results KB
  def getKeyword(model: Model): Option[String] = {

    val query = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?keyword WHERE {
         |?search fs:keyword ?keyword .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()

    if( resultSet.hasNext )
      Some(resultSet.next.getLiteral("keyword").getString)
    else
      None
  }

}
