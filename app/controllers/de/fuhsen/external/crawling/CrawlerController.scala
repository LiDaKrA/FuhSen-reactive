package controllers.de.fuhsen.external.crawling

import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.libs.json._
import play.api.libs.ws.{WSResponse, WSClient}
import play.api.mvc.{Result, Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * Created on 6/30/16.
  */
class CrawlerController @Inject()(ws: WSClient) extends Controller {
  def crawlJobs = Action.async { request =>
    nutchGetRequest(ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job") { response =>
      Ok(response.json)
    } map pickResult
  }

  def crawlJobsByCrawlId(crawlId: String) = Action.async { request =>
    nutchGetRequest(ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job") { response =>
      response.json match {
        case jobs: JsArray =>
          Ok(JsArray(jobs.value.filter(v => (v \ "crawlId").as[String] == crawlId)))
        case other =>
          InternalServerError("Expected JSON array from Nutch service. But got " + other.getClass.getName)
      }
    } map pickResult
  }

  def crawlJobProgressByCrawlId(crawlId: String) = Action.async { request =>
    nutchGetRequest(ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job") { response =>
      response.json match {
        case jobs: JsArray =>
          val jobsByCrawlId = jobs.value.filter(v => (v \ "crawlId").as[String] == crawlId)
          val jobsReducedInformation = jobsByCrawlId map { job =>
            val id = (job \ "id").as[String]
            val msg = (job \ "msg").as[String]
            val `type` = (job \ "type").as[String]
            val status = (job \ "state").as[String]
            JsObject(Seq(
              "id" -> JsString(id),
              "msg" -> JsString(msg),
              "type" -> JsString(`type`),
              "status" -> JsString(status)
            ))
          }
          Ok(JsArray(jobsReducedInformation))
        case other =>
          InternalServerError("Expected JSON array from Nutch service. But got " + other.getClass.getName)
      }
    } map pickResult
  }

  private def pickResult(successOrError: Either[Result, Result]): Result = {
    successOrError match {
      case Left(result) => result
      case Right(result) => result
    }
  }

  def nutchGetRequest[T](url: String, queryParameters: Map[String, String] = Map(), timeoutInMs: Long = 10000)(processing: (WSResponse) => T): Future[Either[T, Result]] = {
    val jobsResponse = ws.url(url).
        withRequestTimeout(timeoutInMs).
        withQueryString(queryParameters.toSeq: _*).
        get()
    jobsResponse map { response =>
      handleHttpErrors(response, processing)
    }
  }

  def nutchPostRequest[T](url: String,
                          postBody: JsValue,
                          queryParameters: Map[String, String] = Map(),
                          timeoutInMs: Long = 10000)
                         (processing: (WSResponse) => T): Future[Either[T, Result]] = {
    val jobsResponse = ws.url(url).
        withRequestTimeout(timeoutInMs).
        withQueryString(queryParameters.toSeq: _*).
        post(postBody)
    jobsResponse map { response =>
      handleHttpErrors(response, processing)
    }
  }

  def handleHttpErrors[T](response: WSResponse,
                          processing: (WSResponse) => T): Either[T, Result] = {
    response.status / 100 match {
      case 2 =>
        Left(processing(response))
      case 3 =>
        ???
      case 4 =>
        Right(InternalServerError("Nutch request seems to be wrong. Nutch REST service returned " + response.status + "\nMessage: " + response.statusText))
      case _ =>
        Right(InternalServerError("Nutch REST service returned status" + response.status + "\nMessage: " + response.statusText))
    }
  }

  private def createNutchJob(crawlId: String, json: JsValue): Future[Either[String, Result]] = {
    nutchPostRequest(ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job/create", json) { response =>
      response.body
    }
  }

  private def createSeedList(crawlId: String, urls: Seq[String]): Future[Either[String, Result]] = {
    val postBody = JsObject(Seq(
      "id" -> JsString(CrawlCounter.seedListIdCounter.getAndIncrement().toString),
      "name" -> JsString("nutch"),
      "seedUrls" -> JsArray(urls.zipWithIndex.map { case (url, count) =>
        JsObject(Seq(
          "id" -> JsNumber(count + 1),
          "seedList" -> JsNull,
          "url" -> JsString(url)
        ))
      })
    ))
    nutchPostRequest(
      ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/seed/create",
      postBody
    ) { result =>
      result.body
    }
  }

  /**
    * Creates a new crawl job given some URIs. Request JSON format:
    *
    * <pre>
    * { "seedURLs": [
    * "http://url1.com",
    * "http://url2.com",
    * ...
    * ]}
    * </pre>
    *
    * @return The crawl id of the crawl job.
    */
  def createCrawlJob() = Action.async { request =>
    request.body.asJson match {
      case Some(json) =>
        (json \ "seedURLs") match {
          case JsDefined(urlsJs: JsArray) =>
            val crawlId = CrawlCounter.getNextCrawlId()
            val urls = urlsJs.value map (jsString => jsString.as[String])
            createSeedListAndHandleResponse(crawlId, urls)
          case other =>
            Future(BadRequest("Wrong JSON format! Property seedURLs must be an array. But it has been " + other.getClass.getName))
        }
      case None =>
        Future(NotAcceptable("Valid content types: text/json, application/json"))
    }
  }

  private def createSeedListAndHandleResponse(crawlId: String, urls: Seq[String]): Future[Result] = {
    createSeedList(crawlId, urls) map { result =>
      result match {
        case Left(seedListPath) =>
          Created("/crawling/jobs/" + crawlId)
        case Right(failureResult) =>
          failureResult
      }
    }
  }
}

object CrawlCounter {
  val seedListIdCounter = new AtomicLong(1)

  val crawlIdCounter = new AtomicLong(1)

  def getNextCrawlId(): String = {
    val count = crawlIdCounter.getAndIncrement()
    val time = System.currentTimeMillis()
    val crawlId = s"crawl-$time-$count"
    crawlId
  }
}