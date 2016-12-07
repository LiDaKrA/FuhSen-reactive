package controllers.de.fuhsen.wrappers

import play.api.libs.json._

/**
  * Contains meta data with regards to a specific search against multiple wrappers.
  */
case class WrapperSearchMetaData(/** The next page values for each wrapper that implements the [[PaginatingApiTrait]] trait. */
                                 nextPageMap: Map[String, String] = Map.empty)

object WrapperSearchMetaData {
  implicit val wrapperSearchMetaDataReads: Reads[WrapperSearchMetaData] = new Reads[WrapperSearchMetaData] {
    override def reads(json: JsValue): JsResult[WrapperSearchMetaData] = {
      val m = (json \ "nextPageMap").validate(Reads.map[String])
      m map WrapperSearchMetaData.apply
    }
  }

  implicit val wrapperSearchMetaDataWrites: Writes[WrapperSearchMetaData] = new Writes[WrapperSearchMetaData] {
    override def writes(obj: WrapperSearchMetaData): JsValue = {
      Json.obj("nextPageMap" -> JsObject(obj.nextPageMap.mapValues(JsString).toSeq))
    }
  }
}