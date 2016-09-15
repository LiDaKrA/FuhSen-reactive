package controllers.de.fuhsen.engine

import com.typesafe.config.ConfigFactory
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

      //ToDo: Replace this with an extension to String class, .toRdfModel
      //Constructing the rdf results graph model
      val textBody = request.body.asText
      val model = RDFUtil.rdfStringToModel(textBody.get, Lang.TURTLE)

      val keyword = getKeywordQuery(model)
      val dataSources = getDataSourceQuery(model)
      val entityTypes = getEntityTypeQuery(model)
      val enabledDataSourcesByTypes = JenaGlobalSchema.getDataSourceByEntityTypes(entityTypes.split(",").map(x => "'"+x+"'").toSet.mkString(","))

      val finalSelectedDataSources = enabledDataSourcesByTypes.intersect(dataSources.split(",").toSet).mkString(",")

      Logger.info("Selected Sources: "+dataSources)
      Logger.info("Selected Entity Types: "+entityTypes)
      Logger.info("Enabled Data Sources By Entity Types: "+enabledDataSourcesByTypes)
      Logger.info("Final Data Sources: "+finalSelectedDataSources)

      if (keyword.isEmpty)
        Ok(textBody.get)

      //Calling the RDF-Wrappers to get the information //engine.microtask.url
      ws.url(ConfigFactory.load.getString("engine.microtask.url")+"/ldw/restApiWrapper/search?query="+keyword+"&wrapperIds="+finalSelectedDataSources).get.map {
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
      Logger.error("No keyword query found in the graph.")
      ""
    }
  }

  private def getDataSourceQuery(model: Model): String = {
    val keywordQuery = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?dataSource WHERE {
         |?search fs:dataSource ?dataSource .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(keywordQuery, model).execSelect()

    if( resultSet.hasNext )
      resultSet.next.getLiteral("dataSource").getString
    else {
      Logger.error("No data sources found in the graph.")
      ""
    }
  }

  private def getEntityTypeQuery(model: Model): String = {
    val keywordQuery = QueryFactory.create(
      s"""
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |SELECT ?entityType WHERE {
         |?search fs:entityType ?entityType .
         |} limit 10
      """.stripMargin)
    val resultSet = QueryExecutionFactory.create(keywordQuery, model).execSelect()

    if( resultSet.hasNext )
      resultSet.next.getLiteral("entityType").getString
    else {
      Logger.error("No data sources found in the graph.")
      ""
    }
  }

}
