package controllers.de.fuhsen.common

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.api.mvc.{AnyContent, Request}
import utils.dataintegration.RDFUtil

sealed trait ApiResponse

case class ApiError(statusCode: Int, errorMessage: String) extends ApiResponse

case class ApiSuccess(responseBody: String) extends ApiResponse

object ModelBodyParser {

  def parse(body: AnyContent): Option[Model] = {
    val textBody = body.asText
    try {
      val model = RDFUtil.rdfStringToModel(textBody.get, Lang.TURTLE)
      Option(model)
    }  catch {
      //case ioe: IOException => ... // more specific cases first !
      case e: Exception => None
    }
  }

}