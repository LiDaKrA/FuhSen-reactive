package controllers.de.fuhsen.engine

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import utils.dataintegration.RDFUtil
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by dcollarana on 5/23/2016.
  * Micro-Task service that calls the RDF-Wrappers to extract the information.
  */
class FederatedQueryController @Inject()(ws: WSClient) extends Controller {

  def execute = Action.async {
    request =>

      Logger.info("Federated Query")

      //Constructing the rdf results graph model
      val textBody = request.body.asText
      val model = RDFUtil.rdfStringToModel(textBody.get, Lang.TURTLE)

      val keyword = getKeywordQuery(model)

      if (keyword.isEmpty)
        Ok(textBody.get)

      //Calling the RDF-Wrappers to get the information
      ws.url("http://localhost:9000/ldw/restApiWrapper/search?query="+keyword+"&wrapperIds=twitter,gkb").get.map {
        response =>
          val wrappersResult = RDFUtil.rdfStringToModel(response.body, Lang.JSONLD)
          model.add(wrappersResult)
          Ok(RDFUtil.modelToTripleString(model, Lang.TURTLE))
      }

  }

  private def getKeywordQuery(model: Model): String = {

    val keywordQuery = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?keyword WHERE {
         |?search fs:keyword ?keyword .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(keywordQuery, model).execSelect()

    if( resultSet.hasNext )
      resultSet.next.getLiteral("keyword").getString
    else {
      //new Exception("No keyword query found in the graph.")
      Logger.error("No keyword query found in the graph.")
      ""
    }

  }

}
