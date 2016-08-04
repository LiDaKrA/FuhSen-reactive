package controllers.de.fuhsen.engine

import java.io.{BufferedReader, FileReader, File}

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{ModelFactory, Model}
import org.apache.jena.riot.Lang
import play.Logger
import utils.dataintegration.RDFUtil

/**
  * Created by dcollarana on 8/3/2016.
  */
trait GlobalSchemaTrait {

  def load (fileReader: FileReader)

  def getModel() : Model

  def getDataSources() : Model

  def getEntityTypes() : Model

  def isLoaded(): Boolean

}

object JenaGlobalSchema extends GlobalSchemaTrait {

  private var model = ModelFactory.createDefaultModel()
  private var loaded = false

  override def load(fileReader: FileReader) {
    if (!this.isLoaded()) {
      model = RDFUtil.rdfReaderToModel(fileReader, Lang.TURTLE)
      loaded = true
    } else
      Logger.error("Model is already loaded")

    /*val br = new BufferedReader(fileReader)
    try {
      val sb = new StringBuilder()
      var line = br.readLine()

      while (line != null) {
        sb.append(line)
        sb.append(System.lineSeparator())
        line = br.readLine()
      }
      var everything = sb.toString()
      Logger.info(everything)
    } finally {
      br.close()
    }*/
  }

  override def isLoaded() : Boolean = {
    loaded
  }

  override def getModel() : Model = {
    model
  }

  override def getDataSources() : Model = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |CONSTRUCT   {
         |?p rdf:type fs:SearchableEntity .
         |?p rdfs:label ?label .
         |?p fs:key ?key .
         |}
         |WHERE {
         |?p rdfs:subClassOf+ fs:SearchableEntity .
         |?p rdfs:label ?label .
         |?p fs:key ?key .
         |FILTER (NOT EXISTS {?node rdfs:subClassOf ?p }) .
         |}
          """.stripMargin)
    QueryExecutionFactory.create(query, model).execConstruct()
  }

  override def getEntityTypes() : Model = {
    val query = QueryFactory.create(
      s"""
         |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
         |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |CONSTRUCT   {
         |?p rdf:type fs:InformationSource .
         |?p rdfs:label ?label .
         |?p fs:key ?key .
         |}
         |WHERE {
         |?p rdf:type fs:InformationSource .
         |?p rdfs:label ?label .
         |?p fs:key ?key .
         |}
          """.stripMargin)
    QueryExecutionFactory.create(query, model).execConstruct()
  }

}
