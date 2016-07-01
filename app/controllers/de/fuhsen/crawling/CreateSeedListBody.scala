package controllers.de.fuhsen.crawling

/**
  * Created on 7/1/16.
  */
case class CreateSeedListBody(id: String, name: String, seedUrls: Seq[SeedUrl])

case class SeedUrl(id: Long, seedList: String, url: String)

