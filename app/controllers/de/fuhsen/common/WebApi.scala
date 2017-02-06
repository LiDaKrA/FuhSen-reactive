package controllers.de.fuhsen.common

import org.apache.jena.rdf.model.{ModelFactory, Model}
import org.apache.jena.riot.Lang
import play.api.Logger
import play.api.mvc.{AnyContent}
import utils.dataintegration.RDFUtil

sealed trait ApiResponse

case class ApiError(statusCode: Int, errorMessage: String) extends ApiResponse

case class ApiSuccess(responseBody: String, nextPage: Option[String] = None) extends ApiResponse

object ModelBodyParser {

  def parse(body: AnyContent): Option[Model] = {
    body.asText match {
      case Some(textBody) =>
        if (textBody.isEmpty) {
          Logger.info("No meta-model sent - string empty")
          None
        }
        else
          Some(parse(textBody))
      case None =>
        Logger.info("No meta-model sent")
        None
    }
  }

  def parse(body: String): Model = {
    try {
      val model = RDFUtil.rdfStringToModel(body, Lang.TURTLE)
      model
    }  catch {
      case e: Exception =>
        Logger.warn("An exception occurred when creating the MetaData from the text body. Returning an empty model")
        ModelFactory.createDefaultModel()
    }
  }

}