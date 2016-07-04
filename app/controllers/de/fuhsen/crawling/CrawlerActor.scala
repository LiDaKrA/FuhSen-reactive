package controllers.de.fuhsen.crawling

import akka.actor.{Props, UntypedActor}
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.crawling.CrawlerActor._
import controllers.de.fuhsen.crawling.JsonFormatters._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import scala.util.{Failure, Success}

/**
  * Actor that manages crawl jobs. A crawl job can have many phases that have to be triggered explicitly.
  * This actor triggers one phase after the other.
  */
class CrawlerActor(ws: WSClient) extends UntypedActor {
  private val log = Logging.getLogger(getContext().system, this)

  override def onReceive(msg: Any): Unit = {
    msg match {
      case StartCrawl(crawlId, seedListPath) =>
        handleStartCrawl(crawlId, seedListPath)
      case createNutchJob@CreateNutchJob(crawlId, typ, timestamp, args) =>
        handleCreateNutchJob(crawlId, typ, timestamp, args)
      case cnj: CheckNutchJob =>
        handleCheckJob(cnj)
    }
  }

  /**
    * Returns the job specific parameter object.
    */
  private def getJobArgs(cnj: CheckNutchJob, nextJobType: NutchJobType): (NutchJobArgs, Option[Long]) = {
    import cnj._
    nextJobType match {
      case InjectType =>
        // Not done here! This only handles the next job arguments
        throw new RuntimeException("This should not be called")
      case GenerateType =>
        // This is the first phase needing a timestamp
        val timestamp = System.currentTimeMillis()
        val args = GenerateArgs(normalize = false, filter = true, crawlId = crawlId, curTime = timestamp, batch = batch(Some(timestamp)))
        (args, Some(timestamp))
      case FetchType =>
        val args = FetchArgs(threads = 50, crawlId = crawlId, batch = batch(timestamp))
        (args, None)
      case ParseType =>
        val args = ParseArgs(crawlId = crawlId, batch = batch(timestamp))
        (args, None)
      case UpdateDbType =>
        val args = UpdateDbArgs(crawlId = crawlId, batch = batch(timestamp))
        (args, None)
      case IndexType =>
        val args = IndexArgs(crawlId = crawlId, batch = batch(timestamp))
        (args, None)
    }
  }

  /** Generate the String for the `batch` field */
  private def batch(timestamp: Option[Long]): String = {
    timestamp.get + "-4430"
  }

  /**
    * Manage the Nutch job. Start next phase if previous one finished. Poll the current phase asynchronously.
    * @param cnj
    */
  private def handleCheckJob(cnj: CheckNutchJob): Unit = {
    import cnj._
    val url = ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job/" + jobId
    ws.url(url).get() map { response =>
      val state = (response.json \ "state").as[String]
      state match {
        case "FINISHED" =>
          log.info(s"Job $jobId has finished!")
          nextJob.get(cnj.`type`) match {
            case Some(nextJobType) =>
              // Start next job
              val (args, newTimestamp) = getJobArgs(cnj, nextJobType)
              self ! CreateNutchJob(crawlId = crawlId, `type` = nextJobType, newTimestamp orElse timestamp, args = args)
            case None =>
            // Crawl job finished
            // TODO: Report to user
          }
        case "FAILED" =>
          log.error(s"Crawl $crawlId failed in phase ${`type`.toString}. Message: " + response.body)
          // Don't start next phase, stop crawl.
        case "STOPPING"|"KILLING"|"KILLED" =>
          log.warning(s"Crawl job $crawlId has been stopped or killed in phase ${`type`.toString}.")
        case "RUNNING"|"IDLE"|"ANY" =>
          log.debug(s"Job $jobId is in state $state")
          getContext().system.scheduler.scheduleOnce(
            500.milliseconds, self, cnj)
      }
    }
  }

  /** Map from current phase to next phase */
  val nextJob = Map[NutchJobType, NutchJobType](
    InjectType -> GenerateType,
    GenerateType -> FetchType,
    FetchType -> ParseType,
    ParseType -> UpdateDbType,
    UpdateDbType -> IndexType
  )

  private def handleStartCrawl(crawlId: String, seedListPath: String): Unit = {
    log.info("Start crawl " + crawlId)
    self ! CreateNutchJob(crawlId = crawlId, `type` = InjectType, timestamp = None, args = InjectArgs(seedListPath))
  }

  private def handleCreateNutchJob(crawlId: String, typ: NutchJobType, timestamp: Option[Long], args: NutchJobArgs): Unit = {
    val argsJson = jobArgsToJson(typ, args)
    ws.url(ConfigFactory.load().getString("crawler.nutch.rest.api.url") + "/job/create").
        post(Json.toJson(NutchJob(argsJson, confId = "default", crawlId = crawlId, `type` = typ.toString))).onComplete { responseTry =>
      responseTry match {
        case Success(response) =>
          val jobId = response.body
          log.info(s"Created $typ Nutch job: $jobId")
          self ! CheckNutchJob(crawlId, typ, jobId, timestamp)
        case Failure(e) =>
          log.warning("Error in getting a response: " + e.getMessage)
      }
    }
  }

  private def jobArgsToJson(typ: NutchJobType, args: NutchJobArgs): JsValue = {
    (typ, args) match {
      case (InjectType, t: InjectArgs) =>
        Json.toJson(t)
      case (GenerateType, t: GenerateArgs) =>
        Json.toJson(t)
      case (FetchType, t: FetchArgs) =>
        Json.toJson(t)
      case (ParseType, t: ParseArgs) =>
        Json.toJson(t)
      case (UpdateDbType, t: UpdateDbArgs) =>
        Json.toJson(t)
      case (IndexType, t: IndexArgs) =>
        Json.toJson(t)
      case _ =>
        ???
    }
  }
}

object CrawlerActor {
  def props(ws: WSClient) = Props(new CrawlerActor(ws))

  case class StartCrawl(crawlId: String, seedListPath: String)

  case class NutchJob(args: JsValue, confId: String, crawlId: String, `type`: String)

  case class CreateNutchJob(crawlId: String, `type`: NutchJobType, timestamp: Option[Long], args: NutchJobArgs)

  case class CheckNutchJob(crawlId: String, `type`: NutchJobType, jobId: String, timestamp: Option[Long])
}

sealed trait NutchJobArgs

case class InjectArgs(seedDir: String) extends NutchJobArgs

case class GenerateArgs(normalize: Boolean, filter: Boolean, crawlId: String, curTime: Long, batch: String) extends NutchJobArgs

case class FetchArgs(threads: Int, crawlId: String, batch: String) extends NutchJobArgs

case class ParseArgs(crawlId: String, batch: String) extends NutchJobArgs

case class UpdateDbArgs(crawlId: String, batch: String) extends NutchJobArgs

case class IndexArgs(crawlId: String, batch: String) extends NutchJobArgs

sealed abstract class NutchJobType(`type`: String) {
  override def toString = `type`
}

case object InjectType extends NutchJobType(`type` = "INJECT")

case object FetchType extends NutchJobType(`type` = "FETCH")

case object GenerateType extends NutchJobType(`type` = "GENERATE")

case object IndexType extends NutchJobType(`type` = "INDEX")

case object ParseType extends NutchJobType(`type` = "PARSE")

case object UpdateDbType extends NutchJobType(`type` = "UPDATEDB")