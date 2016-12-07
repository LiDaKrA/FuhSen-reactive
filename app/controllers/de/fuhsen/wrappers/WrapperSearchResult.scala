package controllers.de.fuhsen.wrappers

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Writes}


/**
  * Created on 12/6/16.
  */
case class WrapperSearchResult(data: JsValue, metaData: WrapperSearchMetaData)

object WrapperSearchResult {
  implicit val wrapperSearchResultWrites: Writes[WrapperSearchResult] = (
      (JsPath \ "data").write[JsValue] and
          (JsPath \ "metaData").write[WrapperSearchMetaData]
      )(unlift(WrapperSearchResult.unapply))
}