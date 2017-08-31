package controllers.de.fuhsen.engine

import java.io.File
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.dataintegration._
import utils.export.ExcelSink
import play.api.mvc.Cookie
import play.api.mvc.Request

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

  def addToFavorites(graphUid: String, uri: String) = Action.async { implicit request =>
    Logger.info("Add to Favorites")
    val favGraphUri = ConfigFactory.load.getString("store.favorites.graph.uri") + "/" + getFavouriteUid()
    Logger.info(s"GraphUri: $favGraphUri Uri: $uri")
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        GraphStoreManager.postModelToStore(getUriModel(uri, model), favGraphUri, ws).map(result => Ok(result))
      case None =>
        Future(InternalServerError("The provided UID has not a model associated in the cache."))
    }
  }

  def getFavorites(graphUid: String) = Action.async { implicit request =>
    Logger.info("Get Favorites")

    val favouriteUid = getFavouriteUid()
    val favGraphUri = ConfigFactory.load.getString("store.favorites.graph.uri") + "/" + favouriteUid

    Logger.info(s"GraphUid: $favGraphUri")
    ws.url(ConfigFactory.load.getString("store.endpoint.sparql.url"))
      .withQueryString("query"-> s"""construct ?s ?p ?o where {GRAPH <$favGraphUri> {?s ?p ?o . } }""")
      .withHeaders("Accept"->"application/n-triples")
      .get
      .map {
        response =>
          val model = RDFUtil.rdfStringToModel(response.body, Lang.NTRIPLES)
          Ok(RDFUtil.modelToTripleString(model, Lang.JSONLD))
      }
  }

  def countFavorites(graphUid: String) = Action.async { implicit request =>
    Logger.info("Count Favorites")
    val favouriteUid = getFavouriteUid()
    Logger.info(s"Graph Uid: $favouriteUid")
    val favGraphUri = ConfigFactory.load.getString("store.favorites.graph.uri") + "/" + favouriteUid
    Logger.info(s"Graph Uri: $favGraphUri")

    ws.url(ConfigFactory.load.getString("store.endpoint.sparql.url"))
      .withQueryString("query"-> s"""select count(?s) where { GRAPH <$favGraphUri> { ?s a <http://vocab.lidakra.de/fuhsen#SearchableEntity> . } }""")
      .withHeaders("Accept"->"application/json")
      .get
      .map {
        response =>
          Logger.info(response.body)
          val _json = response.json
          val count = (((_json \ "results" \ "bindings")(0)) \ "COUNT1" \ "value").as[String]
          Ok(count).withCookies(Cookie("favorites_graph", favouriteUid.toString))
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

  private def getSameAsLinkGraph(uri1: String, uri2: String) : String = {
    s"""
       |<$uri1> <http://vocab.lidakra.de/fuhsen/sameAs> <$uri2> .
       |
      """.stripMargin
  }

  private def getFavouriteUid()(implicit req: Request[Any]) : String = {
    if(req.cookies.get("favorites_graph") == null || req.cookies.get("favorites_graph") == None)  {
      Logger.info("Cookie is created")
      java.util.UUID.randomUUID.toString
    } else {
      Logger.info("Cookie already exits")
      req.cookies.get("favorites_graph").get.value
    }
  }

}
