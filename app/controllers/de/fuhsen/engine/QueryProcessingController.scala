package controllers.de.fuhsen.engine

import play.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.mvc.AnyContent

/**
  * Created by dcollarana on 5/23/2016.
  */
class QueryProcessingController extends Controller {

  def execute = Action {
      request =>  val textBody = request.body.asText
      Logger.info("Expanding Search Query")
      Ok(textBody.get)
  }

}
