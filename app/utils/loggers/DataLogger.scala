package utils.loggers

import com.typesafe.config.ConfigFactory
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.Model
import play.Logger

import scala.collection.JavaConversions.asScalaIterator

/**
  * Created by dcollarana on 7/3/2017.
  */
object GraphLogger {

  val logger = Logger.of(GraphLogger.getClass)

  def log(graph: String) = {
    if (ConfigFactory.load.getBoolean("fuhsen.logger.messages.enabled"))
      logger.debug(graph)

  }
}

object OrganizationLogger {

  val logger = Logger.of(OrganizationLogger.getClass)

  def log(model: Model) = {
    if (ConfigFactory.load.getBoolean("fuhsen.logger.messages.enabled"))
    {
      val query = QueryFactory.create(
        s"""
           |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
           |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
           |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
           |
         |SELECT ?organization
           |WHERE {
           |  ?s a foaf:Organization .
           |  ?s fs:name ?organization
           |}
           |
      """.stripMargin)

      val resultSet = QueryExecutionFactory.create(query, model).execSelect()

      val organizations = resultSet.map { r =>
        if (r.get("organization") != null && r.get("organization").isLiteral)
          r.getLiteral("organization").getString
        else
          ""
      }.filter( p => !p.isEmpty )
        .toList
        .mkString(", ")

      logger.debug(organizations)
    }
  }

}

object LocationLogger {

  val logger = Logger.of(LocationLogger.getClass)

  def log(model: Model) = {
    if (ConfigFactory.load.getBoolean("fuhsen.logger.messages.enabled")) {
      val query = QueryFactory.create(
        s"""
           |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
           |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
           |
         |SELECT ?location
           |WHERE {
           |  {
           |     ?s a ?SearchableEntity .
           |     ?s fs:location ?location
           |  }
           |  UNION
           |  {
           |     ?s a fs:Location .
           |     ?s fs:name ?location .
           |  }
           |}
           |
      """.stripMargin)

      val resultSet = QueryExecutionFactory.create(query, model).execSelect()

      val locations = resultSet.map { r =>
        if (r.get("location") != null && r.get("location").isLiteral)
          r.getLiteral("location").getString
        else
          ""
      }.filter( p => !p.isEmpty )
        .toList
        .mkString(", ")

      logger.debug(locations)
    }
  }

}