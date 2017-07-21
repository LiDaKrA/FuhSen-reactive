package controllers.de.fuhsen.engine

import play.Logger
import play.api.mvc.{Action, Controller}

/**
  * Created by dcollarana on 7/21/2017.
  */
class RdfGraphController extends Controller {

  def mergeEntities(graphUid: String, uri1: String, uri2: String) = Action {
    Logger.info("Merge Entities")
    Logger.info(s"GraphUid: $graphUid Uri 1: $uri1 Uri 2: $uri2")
    Ok
  }

  def addToFavorites(graphUid: String, uri: String) = Action {
    Logger.info("Add to Favorits")
    Logger.info(s"GraphUid: $graphUid Uri: $uri")
    Ok
  }

  def getFavorites(graphUid: String) = Action {
    Logger.info("Get Favorites")
    Logger.info(s"GraphUid: $graphUid")
    Ok
  }

}
