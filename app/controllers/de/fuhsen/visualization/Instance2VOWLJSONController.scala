package controllers.de.fuhsen.visualization

import javax.inject.Inject

import controllers.de.fuhsen.engine.GraphResultsCache
import org.apache.jena.rdf.model.ModelFactory
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

/**
  *
  */
class Instance2VOWLJSONController @Inject()(ws: WSClient) extends Controller {

  def convertGraph(graphUid: String) = Action {
    Logger.info(s"Convert Graph with URI $graphUid")
    //Fetch graph from cache
    GraphResultsCache.getModel(graphUid) match {
      case Some(model) =>
        val json = Instance2VOWLJSON.convertModelToJson(model)
        //Logger.info(json.toString)
        Ok(json).withHeaders(CACHE_CONTROL -> "no-cache, no-store, max-age=0, must-revalidate")
      case None =>
        val model = ModelFactory.createDefaultModel()
        val json = Instance2VOWLJSON.convertModelToJson(model)
        Ok(json).withHeaders(CACHE_CONTROL -> "no-cache, no-store, max-age=0, must-revalidate")
    }

  }

}
