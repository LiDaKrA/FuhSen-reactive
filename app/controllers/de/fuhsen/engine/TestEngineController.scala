package controllers.de.fuhsen.engine

import javax.inject.Inject

import play.Logger
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Created by dcollarana on 6/29/2017.
  */
class TestEngineController @Inject()(ws: WSClient) extends Controller {

  def testEngine(query: String) = Action.async  {

    val data = ""
    val microtaskServer = "http://localhost:9000/fuhsen"

    val futureResponse: Future[WSResponse] = for {
      responseOne <- ws.url(microtaskServer+s"/engine/api/searches?query=$query").post(data)
      responseTwo <- ws.url(microtaskServer+s"/engine/api/searches/${getUID(responseOne.body)}/results?query=$query&sources=elasticsearch,facebook,gkb,gplus,linkedleaks,occrp,pipl,tor2web,twitter,xing,ebay&types=product,document,website,organization,person&entityType=person&exact=false").get
    } yield responseTwo
    //action taken in case of failure
    futureResponse.recover {
      case e: Exception =>
        val exceptionData = Map("error" -> Seq(e.getMessage)) //
        //ws.url(exceptionUrl).post(exceptionData)
        Logger.error(exceptionData.toString())
    }
    futureResponse.map {
      r =>
        Ok(r.body)
    }
  }

  private def getUID(uid: String) : String = {
    Logger.info(s"UID: $uid")
    val jsonBody = Json.parse(uid)
    (jsonBody \ "uid").as[String]
  }

}
