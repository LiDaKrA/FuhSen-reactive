package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Wrapper around the GooglePlus API.
 */
class GooglePlusWrapper extends RestApiWrapperTrait with SilkTransformableTrait {
  override def apiUrl: String = ConfigFactory.load.getString("gplus.user.url")

  override def queryParams: Map[String, String] = Map(
    "key" -> ConfigFactory.load.getString("gplus.app.key")
  )

  /** Returns for a given query string the representation as query parameter. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = Map(
    "query" -> queryString
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = ConfigFactory.load.getString("silk.transformation.task.gplus.person"),
      createSilkTransformationRequestBody(
        basePath = "",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/gplus/{id}"
      )
    ),
    SilkTransformationTask(
      transformationTaskId = ConfigFactory.load.getString("silk.transformation.task.gplus.organization"),
      createSilkTransformationRequestBody(
        basePath = "organizations",
        uriPattern = ""
      )
    )
  )

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  implicit val peopleReader = Json.reads[Person]

  // Returns a JSON array of person objects
  override def customResponseHandling(implicit ws: WSClient) = Some(apiResponse => {
    val people = (Json.parse(apiResponse) \ "items").as[List[Person]]
    for {
      results <- requestAllPeople(people)
    } yield {
      logIfBadRequestsExist(results)
      validResponsesToJSONString(results)
    }
  })

  private def requestAllPeople(people: List[Person])
                              (implicit ws: WSClient): Future[List[WSResponse]] = {
    Future.sequence(people.map { person =>
      //Google plus get person request
      ws.url(apiUrl + "/" + person.id)
          .withQueryString(queryParams.toSeq: _*)
          .get()
    })
  }

  private def validResponsesToJSONString(results: List[WSResponse]): String = {
    val validResults = results.filter(_.status == 200) flatMap { result =>
      if (result.status != 200) {
        Some(result.body)
      } else {
        None
      }
    }
    // Make JSON array
    validResults.mkString("[", ",", "]")
  }

  private def logIfBadRequestsExist(results: List[WSResponse]): Unit = {
    if (results.exists(_.status != 200)) {
      if (results.forall(_.status != 200)) {
        Logger.warn("All requests failed!")
      } else {
        Logger.warn("At least one request failed.")
      }
    }
  }
}