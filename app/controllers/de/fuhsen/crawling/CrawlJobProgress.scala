package controllers.de.fuhsen.crawling

import controllers.de.fuhsen.crawling.CrawlerActor.CrawlStatus


/** The progress of a specific job of a crawl */
case class CrawlJobProgress(id: String, msg: String, `type`: String, status: String)

/** The overall crawl progress */
case class CrawlProgress(crawlId: String, currentJob: String, crawlStatus: String, crawlDetails: Option[CrawlStatus])