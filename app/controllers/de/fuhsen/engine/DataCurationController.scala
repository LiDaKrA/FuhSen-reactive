package controllers.de.fuhsen.engine

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import play.Logger
import play.api.mvc.{Action, Controller}
import utils.dataintegration.RDFUtil

/**
  * Created by dcollarana on 1/2/2017.
  */
class DataCurationController extends Controller {

  def execute = Action {
    request =>
      Logger.info("Data Curation")
      //1. Transform string into model
      //ToDo: Replace this with an extension to String class, .toRdfModel
      //Constructing the rdf results graph model
      val textBody = request.body.asText
      val model = RDFUtil.rdfStringToModel(textBody.get, Lang.TURTLE)

      val keyword = FuhsenVocab.getKeyword(model)
      Logger.info("Keyword: "+keyword)

      //2. Add entities for Person annotations
      val sparqlQuery = QueryFactory.create(
        s"""
           |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
           |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
           |
           |CONSTRUCT {
           | ?uri a fs:Person .
           | ?s fs:annotation_person ?uri .
           | }
           |WHERE {
           |?uri a fs:Annotation .
           |?uri fs:entity-type ?type .
           |?s fs:annotation ?uri .
           | }
      """.stripMargin)
      val annotations = QueryExecutionFactory.create(sparqlQuery, model).execConstruct()

      //3 Add new annotations triples to the results model
      Logger.info("Data Curation Model Size: "+annotations.size())
      model.add(annotations)

      //4 Return the model containing the rank values
      Ok(RDFUtil.modelToTripleString(model, Lang.TURTLE))
  }

}
