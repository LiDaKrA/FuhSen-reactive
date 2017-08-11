package utils.dataintegration

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import org.apache.jena.update.{UpdateAction, UpdateFactory}
import play.Logger

object MergeOperator {

  /*
  Resource Similarity Molecule (RSM) structure
  A RSM = (M, T, Prov) Hear, Tail, Provenance
  For simplicity a Jena Model encapsulates the RMS
  */
  private def createRms(uri: String, model: Model) : Model = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |CONSTRUCT   {
         |<$uri> ?p ?o .
         |}
         |WHERE {
         |<$uri> ?p ?o .
         |}""".stripMargin)
    QueryExecutionFactory.create(query, model).execConstruct()
  }

  def merge(uri_1: String, uri_2: String, model: Model, fusionPolicy: (Model,Model) => Model) : Model = {
    Logger.info("Starting merge operator")
    val merged = fusionPolicy(createRms(uri_1, model), createRms(uri_2, model))
    deleteRms(uri_1, model)
    deleteRms(uri_2, model)
    model.add(merged)
  }

  val unionPolicy = (r1: Model, r2: Model) => r1.add(r2)

  private def deleteRms(uri: String, model: Model) = {
    val query = UpdateFactory.create(s"""
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
        |PREFIX prov: <http://www.w3.org/ns/prov#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |
        |DELETE
        | { <$uri> ?p ?o }
        |WHERE {
        |  <$uri> ?p ?o .
        | }
      """.stripMargin)
    UpdateAction.execute(query, model)
    model
  }

}
