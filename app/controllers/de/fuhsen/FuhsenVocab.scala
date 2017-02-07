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

import java.text.SimpleDateFormat
import java.util.UUID

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{ModelFactory, Model}
import org.apache.jena.riot.Lang
import play.Logger
import utils.dataintegration.RDFUtil
import java.util.Calendar
import scala.collection.JavaConversions.asScalaIterator

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
  //prov
  val PROV_RETRIEVAL_URI = NS + "/" + "search#"


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

  def createProvModel(wrapperName : String, endedByCode: String, endedByReason: String, nextPage : Option[String] ) : Model = {
    Logger.info("Creating prov metadata for: "+wrapperName+" "+endedByCode+" "+endedByReason)

    val now = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("yyyy.MM.dd")
    val currentDateFormat = minuteFormat.format(now)

    val model = ModelFactory.createDefaultModel()
    val irUid = UUID.randomUUID.toString()

    //nextPage
    var nextPageString = ""
    if (!nextPage.isEmpty) {
      Logger.info("Next page added...")
      nextPage.map( value => nextPageString = value )
    }


    val provString = s"""
         |@prefix fs: <http://vocab.lidakra.de/fuhsen#> .
         |@prefix dc: <http://purl.org/dc/elements/1.1/> .
         |@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
         |@prefix xml: <http://www.w3.org/XML/1998/namespace> .
         |@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
         |@prefix prov: <http://www.w3.org/ns/prov#> .
         |@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
         |
         |fs:$wrapperName a prov:Agent;
         |                fs:nextPage "$nextPageString" ;
         |				        rdfs:label "$wrapperName" .
         |
         |fs:$irUid a prov:Activity ;
         |					prov:startedAtTime "$currentDateFormat" ;
         |					prov:endedAtTime "$currentDateFormat" ;
         |					prov:wasAssociatedWith fs:$wrapperName;
         |					prov:wasEndedBy fs:$endedByCode .
         |
         |fs:$endedByCode a prov:Entity ;
         |					      rdfs:label "$endedByCode";
         |					      rdfs:comment "$endedByReason" .
      """.stripMargin
    Logger.info("PROV string: "+provString)
    RDFUtil.rdfStringToModel(provString, Lang.TURTLE)
  }

  def getProvModel (model: Model): Model = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX prov: <http://www.w3.org/ns/prov#>
         |
         |CONSTRUCT   {
         |  ?s a prov:Activity .
         |  ?s ?p ?o .
         |  ?s1 a prov:Agent .
         |  ?s1 ?p1 ?o1 .
         |}
         |WHERE {
         |  ?s a prov:Activity .
         |  ?s ?p ?o .
         |  ?s1 a prov:Agent .
         |  ?s1 ?p1 ?o1 .
         |}
      """.stripMargin)
    QueryExecutionFactory.create(query, model).execConstruct()
  }

  def getProvAgentNextPage (model: Model, wrapperName: String): String = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |PREFIX prov: <http://www.w3.org/ns/prov#>
         |
         |SELECT ?nextPage
         |WHERE {
         |  ?s a prov:Agent .
         |  ?s rdfs:label "$wrapperName" .
         |  ?s fs:nextPage ?nextPage .
         |}
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()

    if(resultSet.hasNext) {
      resultSet.next.getLiteral("nextPage").getString
    }
    else
      ""
  }

  def getWrappersFromMetadata(model: Model) : List[String] = {
    val query = QueryFactory.create(
                s"""
                  |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                  |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                  |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                  |PREFIX prov: <http://www.w3.org/ns/prov#>
                  |
                  |SELECT ?label ?nextPage
                  |WHERE {
                  |  ?s a prov:Agent .
                  |  ?s rdfs:label ?label .
                  |  ?s fs:nextPage ?nextPage .
                  |}
                """.stripMargin)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()
    resultSet.map { r =>
      if (!r.getLiteral("nextPage").getString.isEmpty)
        r.getLiteral("label").getString
      else
        ""
    }.toList
  }

}
