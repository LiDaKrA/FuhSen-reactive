package controllers.de.fuhsen.visualization

import javax.inject.Inject

import org.apache.jena.rdf.model.ModelFactory
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

/**
  *
  */
class Instance2VOWLJSONController @Inject()(ws: WSClient) extends Controller {
  def convertGraph(graph: String) = Action {
    val model = ModelFactory.createDefaultModel() // TODO: Fetch graph from store
    val json = Instance2VOWLJSON.convertModelToJson(model)
    Ok(json).withHeaders(CACHE_CONTROL -> "no-cache, no-store, max-age=0, must-revalidate")
  }
}
