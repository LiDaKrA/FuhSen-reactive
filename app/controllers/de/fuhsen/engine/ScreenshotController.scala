package controllers.de.fuhsen.engine

import javax.inject.Inject
import com.typesafe.config.ConfigFactory
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ResponseHeader, Action, Controller, Result}
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

    val futureResponse: Future[WSResponse] = ws.url(ConfigFactory.load.getString("snapshot.pdf.rest.api.url")).withHeaders("Content-Type" -> "application/json").post(data)

    futureResponse.map {
      r =>
        Result(
          header = ResponseHeader(200),
          body = Enumerator(r.bodyAsBytes)
        ).withHeaders( CONTENT_TYPE -> "application/pdf")
       //Ok(r.body)
    }
  }
}