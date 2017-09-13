package controllers.de.fuhsen.engine

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import utils.dataintegration._
import utils.export.ExcelSink

import scala.concurrent.Future

/**
  * Created by dcollarana on 7/21/2017
  */
class RdfGraphController @Inject()(ws: WSClient) extends Controller {
  implicit private val excelSheetReads = Json.reads[ExcelSheetJson]
  implicit private val excelRequestReads = Json.reads[GenerateExcelJsonRequest]

  def mergeEntities(graphUid: String, uri1: String, uri2: String) = Action {
    Logger.info("Merge Entities")
    Logger.info(s"GraphUid: $graphUid Uri 1: $uri1 Uri 2: $uri2")
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        MergeOperator.merge(uri1, uri2, model, MergeOperator.unionPolicy) match {
          case error: MergeError => InternalServerError(error.errorMessage)
          case success: MergeSuccess =>
            GraphResultsCache.updateModel(graphUid, success.model)
            //GraphStoreManager.postModelToStore(getSameAsLinkGraph(uri1, uri2), ConfigFactory.load.getString("store.same.graph.uri"), "application/n-triples", ws)
            Ok
          case _: NothingToMerge => Ok
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

  def getFavorites(graphUid: String): Action[AnyContent] = Action.async { implicit request =>
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

  def cleanFavorites(graphUid: String) = Action.async { implicit request =>
    Logger.info("Clean Favorites")

    val favouriteUid = getFavouriteUid()
    val favGraphUri = ConfigFactory.load.getString("store.favorites.graph.uri") + "/" + favouriteUid

    Logger.info(s"GraphUid: $favGraphUri")
    ws.url(ConfigFactory.load.getString("store.endpoint.service.url"))
      .withQueryString("graph" -> favGraphUri)
      .delete.map { r =>
      Logger.info("Response Delete Graph: "+r.body)
      Ok(r.body)
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
          val _json = response.json
          val count = ((_json \ "results" \ "bindings")(0) \ "COUNT1" \ "value").as[String]
          Ok(count).withCookies(Cookie("favorites_graph", favouriteUid.toString))
      }
  }

  def exportToExcel(): Action[JsValue] = Action(BodyParsers.parse.json) { implicit request =>
    val parsedResult = request.body.validate[GenerateExcelJsonRequest]
    parsedResult.fold(
      errors => {
        BadRequest(Json.obj("status" -> "JSON parse error", "message" -> JsError.toJson(errors)))
      },
      obj => {
        Ok(ExcelSink.generate(obj).toByteArray).withHeaders("" -> "application/x-download", "Content-disposition" -> "attachment; filename=results.xlsx")
      }
    )
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
    if(req.cookies.get("favorites_graph") == null || req.cookies.get("favorites_graph").isEmpty)  {
      Logger.info("Cookie is created")
      java.util.UUID.randomUUID.toString
    } else {
      Logger.info("Cookie already exits")
      req.cookies.get("favorites_graph").get.value
    }
  }
}

case class GenerateExcelJsonRequest(sheets: Seq[ExcelSheetJson])
case class ExcelSheetJson(name: String, rows: Seq[Seq[String]])