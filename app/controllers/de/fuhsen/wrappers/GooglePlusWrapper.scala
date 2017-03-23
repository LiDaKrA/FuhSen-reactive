/*
 * Copyright (C) 2016 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.wrappers.dataintegration.{SilkTransformableTrait, SilkTransformationTask}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws._
import scala.concurrent.Future

/**
  * Wrapper around the GooglePlus API.
  */

case class Person(id: String, displayName: String, objectType: String)

class GooglePlusWrapper extends RestApiWrapperTrait with SilkTransformableTrait with PaginatingApiTrait {

  //Google+ API implementation
  //override def apiUrl: String = ConfigFactory.load.getString("yql.url")
  //Replaced by YQL access point
  override def apiUrl: String = ConfigFactory.load.getString("gplus.user.url")


  override def queryParams: Map[String, String] = Map (
    // YQL Implementation
    //"format" -> "json"
    //Google+ API implementation
    "key" -> ConfigFactory.load.getString("gplus.app.key"),
    "maxResults" -> "50"
  )

  /** Headers that should be added to the request. */
  override def headersParams: Map[String, String] = Map()

  /** Returns for a given query string the representation as query parameter. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = Map (
    // YQL Implementation
    //"q" -> ("USE 'http://www.datatables.org/google/google.plus.people.search.xml';SELECT * FROM google.plus.people.search WHERE key='"+ConfigFactory.load.getString("gplus.app.key")+"' AND query='"+queryString+"' LIMIT 10")

    //Google+ API implementation
    "query" -> queryString
  )

  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin

  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = ConfigFactory.load.getString("silk.transformation.task.gplus.person"),
      createSilkTransformationRequestBody(
        basePath = "query/results/json",
        //Replaced by YQL
        //basePath = "",
        uriPattern = "http://vocab.lidakra.de/fuhsen/search/entity/gplus/{id}"
      )
    ),
    SilkTransformationTask(
      transformationTaskId = ConfigFactory.load.getString("silk.transformation.task.gplus.organization"),
      createSilkTransformationRequestBody(
        basePath = "query/results/json/organizations",
        //Replaced by YQL
        //basePath = "organizations",
        uriPattern = ""
      )
    ),
    SilkTransformationTask(
      transformationTaskId = ConfigFactory.load.getString("silk.transformation.task.gplus.place"),
      createSilkTransformationRequestBody(
        basePath = "query/results/json/placesLived",
        //Replaced by YQL
        //basePath = "organizations",
        uriPattern = ""
      )
    )
  )

  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")

  implicit val peopleReader = Json.reads[Person]

  // Returns a JSON array of person objects
  override def customResponseHandling(implicit ws: WSClient) = Some(apiResponse => {

    val people = {
      try { (Json.parse(apiResponse) \ "items").as[List[Person]]
        }catch {
          case e: Exception =>
            try {
              List[Person]((Json.parse(apiResponse) \ "items").as[Person]) //
            } catch {
              case e: Exception => List[Person]()
            }
      }
    }



    //Replaced by YQL
    //val people = (Json.parse(apiResponse) \ "items").as[List[Person]]
    //val pages = people.filter(_.objectType == "page")
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
      //Replaced by YQL
      //val _url = apiUrl + "/" + person.id
      val _url = ConfigFactory.load.getString("yql.url") + "?q=USE 'http://www.datatables.org/google/google.plus.people.xml';SELECT * FROM google.plus.people WHERE key='"+ConfigFactory.load.getString("gplus.app.key")+"' AND userId='"+person.id+"' &format=json"
      val request = ws.url(_url)
          .withQueryString(queryParams.toSeq: _*)
      request.get()
    })
  }

  private def validResponsesToJSONString(results: List[WSResponse]): String = {
    val validResults = results.filter(_.status == 200) flatMap { result =>
      if (result.status == 200) {
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
        logPartialRequestFailures(results)
      }
    }
  }

  private def logPartialRequestFailures(results: List[WSResponse]): Unit = {
    val failedRequests = results.filter(_.status != 200)
    val count = failedRequests.size
    val example = failedRequests.head
    Logger.warn(s"$count / ${results.size} requests failed. Example: Status Code: " + example.status + ", Body:" + example.body)
  }

  /**
    * Returns the globally unique URI String of the source that is wrapped. This is used to track provenance.
    */
  override def sourceLocalName: String = "gplus"

  /** The query parameter to specify the page/offset in the result set */
  override def nextPageQueryParameter: String = "nextPageToken"

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    *
    * @param resultBody The body serialized as String as coming from the API.
    * @param apiUrl  The last value. This can be used if the value is not available in the result body, but instead
    *                   is calculated by the wrapper implementation.
    */
  override def extractNextPageQueryValue(resultBody: String, apiUrl: Option[String]): Option[String] = {
    val jsonBody = Json.parse(resultBody)
    val numberOfResults = countIds(resultBody)
    Logger.info("Calculating number of results: "+numberOfResults)
    if (numberOfResults < 50)
      None
    else {
      val nextPageValue = (jsonBody \ "nextPageToken").asOpt[String]
      nextPageValue match {
        case Some(value) =>
          if (apiUrl.get.contains("pageToken")){
            val lastValue = getParameters(apiUrl.get)("pageToken")
            Some(apiUrl.get.replace("pageToken="+lastValue, "pageToken="+value))
          }else{
            Some(apiUrl.get+"&pageToken="+value) //
          }
        case None => None
      }
    }
  }

  private def countIds(body: String) : Int = {
    var count = 0
    var index = body.indexOf("id")
    while (index != -1) {
      count = count + 1
      index = body.indexOf("\"id\"", index + 2)
    }
    count
  }

  private def getParameters(url: String) : Map[String,String] = {
    val url2 = url.split("\\?").last
    url2.split("&") map { t =>
      val idx = t.indexOf("=")
      t.substring(0, idx) -> t.substring(idx + 1) } toMap
  }

}