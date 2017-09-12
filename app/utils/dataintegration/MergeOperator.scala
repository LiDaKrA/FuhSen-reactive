package utils.dataintegration

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.update.{UpdateAction, UpdateFactory}
import play.Logger

sealed trait MergeResponse
case class MergeError(errorMessage: String) extends MergeResponse
case class MergeSuccess(model: Model) extends MergeResponse
case class NothingToMerge(message: String) extends MergeResponse

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

  def merge(uri_1: String, uri_2: String, model: Model, fusionPolicy: (Model,Model) => Model) : MergeResponse = {
    Logger.info("Starting merge operator")
    try {
      val rms_1 = createRms(uri_1, model)
      if (rms_1.size == 0)
        NothingToMerge("Molecule 1 not found")
      val rms_2 = createRms(uri_2, model)
      if (rms_2.size == 0)
        NothingToMerge("Molecule 1 not found")
      val merged = fusionPolicy(rms_1, rms_2)

      //Applying some tricks
      val model_1 = deleteRms(uri_1, model)
      val model_2 = deleteRms(uri_2, model_1)

      val finalModel = ModelFactory.createDefaultModel()
      finalModel.add(model_2.add(merged))

      MergeSuccess(finalModel)

    } catch {
      case e: Exception => MergeError(e.getMessage)
    }
  }

  val unionPolicy = (r1: Model, r2: Model) => {
    r1.add(r2)
    val uid = java.util.UUID.randomUUID.toString
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |CONSTRUCT   {
         |<http://vocab.lidakra.de/fuhsen/search/merged_entity/$uid> ?p ?o .
         |}
         |WHERE {
         |?s ?p ?o .
         |}""".stripMargin)
    QueryExecutionFactory.create(query, r1).execConstruct()
  }

  private def deleteRms(uri: String, model: Model) : Model = {
    val query = UpdateFactory.create(s"""
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
