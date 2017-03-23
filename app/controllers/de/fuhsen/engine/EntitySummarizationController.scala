package controllers.de.fuhsen.engine

import play.Logger
import play.api.mvc.{AnyContent, Action, Controller}
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.riot.Lang
import utils.dataintegration.RDFUtil
/**
  * Created by dcollarana on 5/23/2016.
  */
class EntitySummarizationController extends Controller {

  def execute = Action {
    request =>
      val textBody = request.body.asText
      Logger.info("Entity Summarization")
      Ok(textBody.get)
  }
  def summarizeEntity(uid: String, entityType: String, uri: String) = Action {

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        val query = entityType match {
          case "person" =>
            QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
             |CONSTRUCT   {
                 |  <$uri> ?p ?o .
                 |  <$uri> rdf:type foaf:Person .
                 |  <$uri> fs:title ?name .
                 |  <$uri> fs:source ?source .
                 |  <$uri> fs:rank ?rank .
                 |  <$uri> fs:image ?img .
                 |  <$uri> fs:url ?url .
                 |}
                 |WHERE {
                 |<$uri> rdf:type foaf:Person .
                 |<$uri> fs:name ?name .
                 |<$uri> fs:source ?source .
                 |<$uri> fs:rank ?rank .
                 |OPTIONAL { <$uri> fs:url ?url } .
                 |OPTIONAL { <$uri> fs:img ?img } .
                 |OPTIONAL {
                 |    { <$uri> ?p ?o .
                 |    FILTER(isLiteral(?o))
                 |    }
                 |  UNION
                 |    { <$uri> ?p ?resource .
                 |    ?resource fs:name ?o .
                 |    FILTER(isURI(?resource))
                 |    }
                 |  }
                 |  }
                  """.stripMargin)
          case "product" =>
            QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |CONSTRUCT   {
                 |<$uri> rdf:type gr:ProductOrService .
                 |<$uri> fs:title ?description .
                 |<$uri> fs:image ?img .
                 |<$uri> fs:url ?url .
                 |<$uri> fs:location ?location .
                 |<$uri> fs:country ?country .
                 |<$uri> fs:priceLabel ?price .
                 |<$uri> fs:condition ?condition .
                 |<$uri> fs:source ?source .
                 |}
                 |WHERE {
                 |<$uri> rdf:type gr:ProductOrService .
                 |<$uri> fs:description ?description .
                 |<$uri> fs:source ?source .
                 |OPTIONAL { <$uri> fs:img ?img } .
                 |OPTIONAL { <$uri> fs:url ?url } .
                 |OPTIONAL { <$uri> fs:location ?location } .
                 |OPTIONAL { <$uri> fs:country ?country } .
                 |OPTIONAL { <$uri> fs:priceLabel ?price } .
                 |OPTIONAL { <$uri> fs:condition ?condition } .
                 |}
                 """.stripMargin)

          case "organization" =>
            QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |CONSTRUCT   {
                 |<$uri> ?p ?o .
                 |<$uri> rdf:type foaf:Organization .
                 |<$uri> fs:title ?name .
                 |<$uri> fs:image ?img .
                 |<$uri> fs:url ?url .
                 |<$uri> fs:source ?source .
                 |}
                 |WHERE {
                 |<$uri> rdf:type foaf:Organization .
                 |<$uri> fs:name ?name .
                 |<$uri> fs:source ?source .
                 |OPTIONAL {<$uri> fs:url ?url } .
                 |OPTIONAL { <$uri> fs:img ?img } .
                 |OPTIONAL { <$uri> ?p ?o .
                 |FILTER(isLiteral(?o)) }
                 |}
                   """.stripMargin)
        }
        val results_model = QueryExecutionFactory.create(query, model).execConstruct()
        Ok(RDFUtil.modelToTripleString(results_model, Lang.JSONLD))
      case None =>
        InternalServerError(
          "The provided UID has not a model associated in the cache.")
    }
  }
}
