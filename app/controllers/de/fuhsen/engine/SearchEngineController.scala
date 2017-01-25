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

package controllers.de.fuhsen.engine

import java.util.{Calendar, UUID}
import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.riot.Lang
import utils.dataintegration.RDFUtil
import scala.concurrent.Future
import javax.inject.Inject
import org.apache.jena.rdf.model.{Model, ModelFactory}
import play.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

class SearchEngineController @Inject()(ws: WSClient) extends Controller {

  def search(uid: String, entityType: String, facets: Option[String], sources: String, types: String) = Action.async { request =>

    Logger.info("Starting Search Engine Search : "+uid)
    Logger.info("Sources : "+sources+" types: "+types)

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>

        if (getQueryDate(model) == null) {
          //Search results are not in storage, searching process starting

          //Adding query date property
          model.getResource(FuhsenVocab.SEARCH_URI + uid).addProperty(model.createProperty(FuhsenVocab.QUERY_DATE), Calendar.getInstance.getTime.toString)
          model.getResource(FuhsenVocab.SEARCH_URI + uid).addProperty(model.createProperty(FuhsenVocab.DATA_SOURCE), sources)
          model.getResource(FuhsenVocab.SEARCH_URI + uid).addProperty(model.createProperty(FuhsenVocab.ENTITY_TYPE), types)


          //Micro-task services executed
          val data = RDFUtil.modelToTripleString(model, Lang.TURTLE)
          val microtaskServer = ConfigFactory.load.getString("engine.microtask.url")
          val futureResponse: Future[WSResponse] = for {
            //responseOne <- ws.url(microtaskServer+"/engine/api/queryprocessing").post(data)
            responseOne <- ws.url(microtaskServer+"/engine/api/federatedquery").post(data)
            responseTwo <- ws.url(microtaskServer+"/engine/api/datacuration").post(responseOne.body)
            responseThree <- ws.url(microtaskServer+"/engine/api/entitysummarization").post(responseTwo.body)
            responseFour <- ws.url(microtaskServer+"/engine/api/semanticranking").post(responseThree.body)
          } yield responseFour
          //action taken in case of failure
          futureResponse.recover {
            case e: Exception =>
              val exceptionData = Map("error" -> Seq(e.getMessage))
              //ws.url(exceptionUrl).post(exceptionData)
              Logger.error(exceptionData.toString())
          }
          futureResponse.map {
            r =>
              if(!r.body.equals("NO VALID TOKEN")){
                val finalModel = RDFUtil.rdfStringToModel(r.body, Lang.TURTLE)
                GraphResultsCache.saveModel(uid, finalModel)
                Logger.info("Search results stored in cache: "+uid)

                //Return sub model by type
                Ok(RDFUtil.modelToTripleString(getSubModel(entityType, finalModel), Lang.JSONLD))
              }else{
                NotAcceptable(r.body)
              }
          }
        }
        //Search results are already in storage
        else {
          Logger.info("Results are in cache.")
          Future.successful(Ok(RDFUtil.modelToTripleString(getSubModel(entityType, model), Lang.JSONLD)))
        }


      case None =>
        Future.successful(InternalServerError("Provided uid has not result model associated."))
    }

  }

  def calculateModelStat(uid: String) = Action { request =>
    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
             |
             |SELECT (SAMPLE(?type) AS ?entity_type) (SAMPLE(?key) AS ?entity_key) (COUNT(?type) AS ?count)
             |WHERE {
             |  {
             |  ?s rdf:type ?type .
             |  ?type fs:key ?key .
             |  ?s fs:name ?name .
             |  ?s fs:source ?source .
             |  ?s fs:rank ?rank .
             |  FILTER(?key = "person") .
             |  }
             |  UNION
             |  {
             |    ?s rdf:type ?type .
             |    ?type fs:key ?key .
             |    ?s fs:name ?name .
             |    ?s fs:source ?source .
             |    FILTER(?key = "organization") .
             |  }
             |  UNION
             |  {
             |    ?s rdf:type ?type .
             |    ?type fs:key ?key .
             |    ?s fs:description ?description .
             |    ?s fs:source ?source .
             |    FILTER(?key = "product") .
             |  }
             |    UNION
             |  {
             |    ?s rdf:type ?type .
             |    ?type fs:key ?key .
             |    ?s fs:source ?source .
             |    FILTER(?key = "website") .
             |  }
             |   UNION
             |  {
             |    ?s rdf:type ?type .
             |    ?type fs:key ?key .
             |    ?s fs:label ?label .
             |    FILTER(?key = "document") .
             |  }
             |}
             |GROUP BY ?type ?key
          """.stripMargin)
        val resultSet = QueryExecutionFactory.create(query, model).execSelect()
        val results_model = ModelFactory.createDefaultModel()

        while(resultSet.hasNext) {
          val result = resultSet.next
          if(result.getResource("entity_type") != null) {
            val entity_type = result.getResource("entity_type").getURI
            val count = result.getLiteral("count").getString
            val entity_key = result.getLiteral("entity_key").getString
            val resource = results_model.createResource(entity_type)
            resource.addProperty(results_model.createProperty(FuhsenVocab.FACET_VALUE), entity_key)
            resource.addProperty(results_model.createProperty(FuhsenVocab.FACET_COUNT), count)
          }
        }
        Ok(RDFUtil.modelToTripleString(results_model, Lang.JSONLD))
      case None =>
        InternalServerError("The provided UID has not a model associated in the cache.")
    }
  }
  def startSession(query: String) = Action { request =>
    Logger.info("Starting Search Session with Query: " + query)

    val searchUid = UUID.randomUUID.toString()
    val searchURI = FuhsenVocab.SEARCH_URI + searchUid

    val model = ModelFactory.createDefaultModel()
    model.add(JenaGlobalSchema.getModel())

    //Creating fs:Search resource
    model.createResource(searchURI)
      .addProperty(model.createProperty(FuhsenVocab.UID), searchUid)
      .addProperty(model.createProperty(FuhsenVocab.KEYWORD), query)
      //.addProperty(model.createProperty(FuhsenVocab.QUERY_DATE), Calendar.getInstance.getTime.toString)

    //Storing in session the new search unique identifier
    GraphResultsCache.saveModel(searchUid, model)
    Logger.info("Number of searches: "+GraphResultsCache.size)
    //request.session + ("SearchUid" -> searchUid)
    //cache.set(searchUid, model, 1.hours)

    Logger.info("Search Session started: "+searchUid)

    val queryJsValue = query.replace("\"","\\\"") //Replacing "" with scape characters for JSON response
    //Building json response
    val json: JsValue = Json.parse(s"""
        {
          "keyword" : "$queryJsValue",
          "uid" : "$searchUid"
        }
        """)
    Ok(json)
  }

  def stopSession(searchUid: String) = Action { request =>
    Logger.info("Stopping Search Session: " + searchUid)

    GraphResultsCache.deleteModel(searchUid)
    Logger.info("Number of searches: "+GraphResultsCache.size)
    Ok

  }

  private def getSubModel(entityType :String, model :Model) : Model = {

    entityType match {
      case "person" =>
                val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
             |
             |CONSTRUCT   {
             |  ?s ?p ?o .
             |  ?s rdf:type foaf:Person .
             |  ?s fs:title ?name .
             |  ?s fs:source ?source .
             |  ?s fs:rank ?rank .
             |  ?s fs:image ?img .
             |  ?s fs:url ?url .
             |}
             |WHERE {
             |?s rdf:type foaf:Person .
             |?s fs:name ?name .
             |?s fs:source ?source .
             |?s fs:rank ?rank .
             |OPTIONAL { ?s fs:url ?url } .
             |OPTIONAL { ?s fs:img ?img } .
             |OPTIONAL {
             |    { ?s ?p ?o .
             |    FILTER(isLiteral(?o))
             |    }
             |  UNION
             |    { ?s ?p ?resource .
             |    ?resource fs:name ?o .
             |    FILTER(isURI(?resource))
             |    }
             |  }
             |}
          """.stripMargin)
        QueryExecutionFactory.create(query, model).execConstruct()
      case "product" =>
        val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |PREFIX gr: <http://purl.org/goodrelations/v1#>
             |
             |CONSTRUCT   {
             |?p rdf:type gr:ProductOrService .
             |?p fs:title ?description .
             |?p fs:image ?img .
             |?p fs:url ?url .
             |?p fs:location ?location .
             |?p fs:country ?country .
             |?p fs:priceLabel ?price .
             |?p fs:condition ?condition .
             |?p fs:source ?source .
             |}
             |WHERE {
             |?p rdf:type gr:ProductOrService .
             |?p fs:description ?description .
             |?p fs:source ?source .
             |OPTIONAL { ?p fs:img ?img } .
             |OPTIONAL { ?p fs:url ?url } .
             |OPTIONAL { ?p fs:location ?location } .
             |OPTIONAL { ?p fs:country ?country } .
             |OPTIONAL { ?p fs:priceLabel ?price } .
             |OPTIONAL { ?p fs:condition ?condition } .
             |}
                  """.stripMargin)
        QueryExecutionFactory.create(query, model).execConstruct()
      case "organization" =>
        val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
             |
             |CONSTRUCT   {
             |?s ?p ?o .
             |?s rdf:type foaf:Organization .
             |?s fs:title ?name .
             |?s fs:image ?img .
             |?s fs:url ?url .
             |?s fs:source ?source .
             |}
             |WHERE {
             |?s rdf:type foaf:Organization .
             |?s fs:name ?name .
             |?s fs:source ?source .
             |OPTIONAL { ?s fs:url ?url } .
             |OPTIONAL { ?s fs:img ?img } .
             |OPTIONAL { ?s ?p ?o .
             |     FILTER(isLiteral(?o)) }
             |
             |}
          """.stripMargin)
        QueryExecutionFactory.create(query, model).execConstruct()
      case "website" =>
        val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |
             |CONSTRUCT   {
             |?s ?p ?o .
             |?s rdf:type foaf:Document .
             |?s fs:url ?url .
             |}
             |WHERE {
             |?s a foaf:Document .
             |?s fs:source ?source .
             |OPTIONAL { ?s fs:url ?url } .
             |OPTIONAL {
             |    { ?s ?p ?o .
             |    FILTER(isLiteral(?o))
             |    }
             |  UNION
             |    { ?s ?p ?resource .
             |    ?resource fs:name ?o .
             |    FILTER(isURI(?resource))
             |    }
             |  }
             |}
          """.stripMargin)
        QueryExecutionFactory.create(query, model).execConstruct()
      case "document" =>
        val query = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |
             |CONSTRUCT   {
             |?s ?p ?o .
             |?s rdf:type fs:Document .
             |?s fs:label ?label .
             |?s fs:url ?url .
             |}
             |WHERE {
             |?s rdf:type fs:Document .
             |?s fs:label ?label .
             |OPTIONAL { ?s ?p ?o .
             |     FILTER(isLiteral(?o)) } .
             |OPTIONAL { ?s fs:url ?url } .
             }
          """.stripMargin)
        QueryExecutionFactory.create(query, model).execConstruct()
    }

  }

  private def getQueryDate(model: Model): String = {

    val query = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?queryDate WHERE {
         |?search fs:queryDate ?queryDate .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()

    if( resultSet.hasNext )
      resultSet.next.getLiteral("queryDate").getString
    else
      null
  }

}

