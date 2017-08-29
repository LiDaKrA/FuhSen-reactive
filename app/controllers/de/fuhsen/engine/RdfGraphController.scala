package controllers.de.fuhsen.engine

import java.io.File
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
import utils.dataintegration._
import utils.export.ExcelSink

/**
  * Created by dcollarana on 7/21/2017
  */
class RdfGraphController @Inject()(ws: WSClient) extends Controller {

  def mergeEntities(graphUid: String, uri1: String, uri2: String) = Action {
    Logger.info("Merge Entities")
    Logger.info(s"GraphUid: $graphUid Uri 1: $uri1 Uri 2: $uri2")
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        MergeOperator.merge(uri1, uri2, model, MergeOperator.unionPolicy) match {
          case error: MergeError => InternalServerError(error.errorMessage)
          case success: MergeSuccess =>
            GraphResultsCache.updateModel(graphUid, success.model)
            GraphStoreManager.postModelToStore(getSameAsLinkGraph(uri1, uri2), ConfigFactory.load.getString("store.same.graph.uri"), "application/n-triples", ws)
            Ok
          case nothigToMerge: NothingToMerge => Ok
        }
      case None =>
        InternalServerError("Provided uid has not result model associated.")
    }
  }

  def addToFavorites(graphUid: String, uri: String) = Action.async {
    Logger.info("Add to Favorites")
    Logger.info(s"GraphUid: $graphUid Uri: $uri")
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        GraphStoreManager.postModelToStore(getUriModel(uri, model), ConfigFactory.load.getString("store.graph.uri"), ws).map(result => Ok(result))
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

  def exportToExcel(graphUid: String, item: String) = Action {
    Logger.info(s"Exporting $item to excel from $graphUid")
    Ok(ExcelSink.generate().toByteArray).withHeaders("" -> "application/x-download", "Content-disposition" -> "attachment; filename=results.xlsx")
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

  /*
  private def push2Dydra(model: String, graph: String): Future[String] = {
    //val model =
    push2Dydra(model, graph)
  }

  private def push2Dydra(model: Model, graph: String): Future[String] = {
    Logger.info(s"Pushing ${model.size} triples to favorits to graph: $graph ")
    val n3_model = RDFUtil.modelToTripleString(model, Lang.N3)
    ws.url(ConfigFactory.load.getString("store.endpoint.service.url"))
        .withQueryString("graph"-> graph).withHeaders("Content-Type"->"text/rdf+n3").post(n3_model).map(
        response => "DYDRA.RESPONSE: "+response.body)
  }
  */

  private def getSameAsLinkGraph(uri1: String, uri2: String) : String = {
    s"""
       |<$uri1> <http://vocab.lidakra.de/fuhsen/sameAs> <$uri2> .
       |
      """.stripMargin
  }

}
