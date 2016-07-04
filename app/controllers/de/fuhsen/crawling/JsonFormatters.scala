package controllers.de.fuhsen.crawling

import controllers.de.fuhsen.crawling.CrawlerActor.NutchJob
import play.api.libs.json.Json

/**
  * Created on 7/1/16.
  */
object JsonFormatters {
  implicit val seedUrlFormatter = Json.format[SeedUrl]
  implicit val createSeedListBodyFormatter = Json.format[CreateSeedListBody]
  implicit val nutchJobProgressFormatter = Json.format[CrawlJobProgress]
  implicit val injectArgsFormatter = Json.format[InjectArgs]
  implicit val generateArgsFormatter = Json.format[GenerateArgs]
  implicit val fetchArgsFormatter = Json.format[FetchArgs]
  implicit val parseArgsFormatter = Json.format[ParseArgs]
  implicit val updateDbArgsFormatter = Json.format[UpdateDbArgs]
  implicit val indexArgsFormatter = Json.format[IndexArgs]
  implicit val nutchJobFormatter = Json.format[NutchJob]
  implicit val crawlProgressFormatter = Json.format[CrawlProgress]
}
