package controllers.de.fuhsen.engine

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang
import play.Logger
import play.api.mvc.{AnyContent, Action, Controller}
import utils.dataintegration.RDFUtil

/**
  * Created by dcollarana on 5/23/2016.
  */
class SemanticRankingController extends Controller {

  def execute = Action {
    request =>
      Logger.info("Semantic Ranking")
      //1. Transform string into model
      //ToDo: Replace this with an extension to String class, .toRdfModel
      //Constructing the rdf results graph model
      val textBody = request.body.asText

      if(!textBody.get.equals("NO VALID TOKEN")){
        val model = RDFUtil.rdfStringToModel(textBody.get, Lang.TURTLE)

        val keyword = getKeyword(model)
        Logger.info("Keyword: "+keyword)

        //2. Add rank property
        //2.0 For Persons
        //2.1 Execute query
        val keywordQuery = QueryFactory.create(
          s"""
             |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
             |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
             |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
             |SELECT ?person ?name ?source
             |WHERE {
             |OPTIONAL { ?person rdf:type foaf:Person . } .
             |OPTIONAL { ?person rdf:type foaf:Organization . } .
             |?person foaf:name ?name .
             |?person fs:source ?source .
             |}
      """.stripMargin)
        val resultSet = QueryExecutionFactory.create(keywordQuery, model).execSelect()

        //2.2 Iterate on the results
        val rankForPersonsModel = ModelFactory.createDefaultModel()
        while(resultSet.hasNext) {
          val result = resultSet.next
          val name = result.getLiteral("name").getString
          val source = result.getLiteral("source").getString
          val resource = rankForPersonsModel.createResource(result.getResource("person").getURI)
          //2.3 Add rank property value
          if (name.contains(keyword))
            if (source == "GoogleKG")
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "1")
            else if (source == "Twitter")
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "2")
            else if (source == "GooglePlus")
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "2")
            else
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "3")
          else {
            if (source == "Twitter")
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "6")
            else if (source == "GoogleKG")
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "7")
            else
              resource.addProperty(rankForPersonsModel.createProperty(FuhsenVocab.RANK), "5")
          }
        }

        //3 Add ranked triples to the results model
        Logger.info("Rank Model Size: "+rankForPersonsModel.size())
        model.add(rankForPersonsModel)

        //4 Return the model containing the rank values
        Ok(RDFUtil.modelToTripleString(model, Lang.TURTLE))
      }else{
        Ok(textBody.get)
      }
  }

  private def getKeyword(model: Model): String = {

    val query = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?keyword WHERE {
         |?search fs:keyword ?keyword .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()

    if( resultSet.hasNext )
      resultSet.next.getLiteral("keyword").getString
    else
      null
  }

}
