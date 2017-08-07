package controllers.de.fuhsen.engine

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.dataintegration.RDFUtil

/**
  * Created by dcollarana on 7/21/2017.
  */
class RdfGraphController @Inject()(ws: WSClient) extends Controller {

  def mergeEntities(graphUid: String, uri1: String, uri2: String) = Action {
    Logger.info("Merge Entities")
    Logger.info(s"GraphUid: $graphUid Uri 1: $uri1 Uri 2: $uri2")
    Ok
  }

  def addToFavorites(graphUid: String, uri: String) = Action.async {
    Logger.info("Add to Favorites")
    Logger.info(s"GraphUid: $graphUid Uri: $uri")
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        push2Dydra(getUriModel(uri, model), ConfigFactory.load.getString("store.graph.uri") ).map(result => Ok(result))
      case None =>
        Future(InternalServerError("The provided UID has not a model associated in the cache."))
    }
  }

  def getFavorites(graphUid: String) = Action.async {
    Logger.info("Get Favorites")
    Logger.info(s"GraphUid: $graphUid")
    ws.url(ConfigFactory.load.getString("store.endpoint.sparql.url"))
      .withQueryString("query"->ConfigFactory.load.getString("store.favorites.sparql"))
      .withHeaders("Accept"->"application/n-triples")
      .get
      .map {
        response =>
          val model = RDFUtil.rdfStringToModel(response.body, Lang.NTRIPLES)
          Ok(RDFUtil.modelToTripleString(model, Lang.JSONLD))
      }
  }

  def countFavorites(graphUid: String) = Action.async {
    Logger.info("Count Favorites")
    Logger.info(s"GraphUid: $graphUid")
    ws.url(ConfigFactory.load.getString("store.endpoint.sparql.url"))
      .withQueryString("query"->ConfigFactory.load.getString("store.favorites.count.sparql"))
      .withHeaders("Accept"->"application/json")
      .get
      .map {
        response =>
          val _json = response.json
          val count = (((_json \ "results" \ "bindings")(0)) \ "COUNT1" \ "value").as[String]
          Ok(count)
      }
  }

  private def getUriModel(uri: String, model: Model) : Model = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |CONSTRUCT   {
         |<$uri> ?p ?o .
         |}
         |WHERE {
         |<$uri> ?p ?o .
      }""".stripMargin)
    QueryExecutionFactory.create(query, model).execConstruct()
  }

  private def push2Dydra(model: Model, graph: String): Future[String] = {
    Logger.info(s"Pushing ${model.size} triples to favorits to graph: $graph ")
    val n3_model = RDFUtil.modelToTripleString(model, Lang.N3)
    ws.url(ConfigFactory.load.getString("store.endpoint.service.url"))
        .withQueryString("graph"-> graph).withHeaders("Content-Type"->"text/rdf+n3").post(n3_model).map(
        response => "DYDRA.RESPONSE: "+response.body)
  }

}
