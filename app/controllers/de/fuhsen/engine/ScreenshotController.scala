package controllers.de.fuhsen.engine

import javax.inject.Inject
import play.api.mvc.{Action, Controller}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import scala.concurrent.Future
/**
  * Created by dcollarana on 8/3/2016.
  */
class ScreenshotController @Inject()(ws: WSClient) extends Controller {

  def screenshots(url: String) = Action.async { request =>
    val data = Json.obj(
      "url" -> url
    )

    print(url)

    val futureResponse: Future[WSResponse] = ws.url("http://localhost:3000/snapshot").withHeaders("Content-Type" -> "application/json").post(data)

    futureResponse.map {
      r =>
        Ok(r.body)
    }
  }
}