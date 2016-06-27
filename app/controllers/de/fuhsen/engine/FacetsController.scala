package controllers.de.fuhsen.engine

import javax.inject.Inject

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import utils.dataintegration.RDFUtil

import scala.concurrent.Future

/**
  * Created by dcollarana on 6/23/2016.
  */
class FacetsController @Inject()(ws: WSClient) extends Controller {

  def getFacets(uid: String, entityType: String) = Action { request =>
    Logger.info("Facets for search : " + uid + " entityType: "+entityType)

    val model = ModelFactory.createDefaultModel()

    entityType match {
      case "person" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Gender")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "gender")
        model.createResource(FuhsenVocab.FACET_URI + "Birthday")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "birthday")
        model.createResource(FuhsenVocab.FACET_URI + "Occupation")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "occupation")
        model.createResource(FuhsenVocab.FACET_URI + "LiveIn")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "livein")
        model.createResource(FuhsenVocab.FACET_URI + "WorkFor")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "workfor")
        model.createResource(FuhsenVocab.FACET_URI + "Studies")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "studies")
      case "organization" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Location")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        model.createResource(FuhsenVocab.FACET_URI + "Country")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
      case "product" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Price")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "price")
        model.createResource(FuhsenVocab.FACET_URI + "Country")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
        model.createResource(FuhsenVocab.FACET_URI + "Location")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
      case _ =>
    }

    Ok(RDFUtil.modelToTripleString(model, Lang.JSONLD))

  }

  def getFacetValues(uid: String, facet: String, entityType :String) = Action { request =>
    Logger.info("Facets values for : " + facet + " uid: "+uid+" entityType: "+entityType)

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        Logger.info("Model size: "+model.size())
        val subModel = getSubModelWithFacet(facet, entityType, model)
        Logger.info("Facet SubModel size: "+subModel.size())
        Ok(RDFUtil.modelToTripleString(subModel, Lang.JSONLD))
      case None =>
        InternalServerError("Provided uid has not result model associated.")
    }

  }

  private def getSubModelWithFacet(facet: String, entityType :String, model :Model) : Model = {

    entityType match {
      case "person" =>
        facet match {
          case "gender" =>
            Logger.info("Executing Construct Query")
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |CONSTRUCT   {
                 |?uri rdf:type fs:FacetValue .
                 |?uri rdfs:label ?facetLabel .
                 |?uri fs:count ?nelements .
                 |}
                 |WHERE {
                 | SELECT (SAMPLE(?localname) AS ?uri) (SAMPLE(?gender) AS ?facetLabel) (COUNT(?gender) as ?nelements)
                 |  WHERE {
                 |    ?p rdf:type foaf:Person .
                 |    ?p foaf:gender ?gender .
                 |    BIND(CONCAT("http://vocab.lidakra.de/facet#", REPLACE(str(?gender), " ", "")) AS ?localname) .
                 |  } GROUP BY ?gender
                 |}
          """.stripMargin)
            QueryExecutionFactory.create(query, model).execConstruct()

        }
    }

  }

}
