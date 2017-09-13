package utils.dataintegration

import com.typesafe.config.ConfigFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by dcollarana on 6/24/2017.
  **/
object GraphStoreManager {

  def postModelToStore(model: Model, graph: String, ws: WSClient): Future[String] = {

    val n3Model = RDFUtil.modelToTripleString(model, Lang.N3)
    postModelToStore(n3Model, graph, "text/rdf+n3", ws)
  }

  def postModelToStore(model: String, graph: String, contentType: String = "application/n-triples", ws: WSClient): Future[String] = {
    Logger.info(s"Pushing ${model.size} triples to graph: $graph ")
    //returning store response
    ws.url(ConfigFactory.load.getString("store.endpoint.service.url"))
        .withQueryString("graph"-> graph)
        .withHeaders("Content-Type" -> contentType)
        .post(model).map{ response =>
          Logger.info("STORE RESPONSE"+response.body)
          "STORE.RESPONSE: "+response.body
    }
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

}
