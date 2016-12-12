package controllers.de.fuhsen.engine

import javax.inject.Inject

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{ResultSet, QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import utils.dataintegration.RDFUtil
import scala.collection.JavaConversions.asScalaIterator
/**
  * Created by dcollarana on 6/23/2016.
  */
class FacetsController @Inject()(ws: WSClient) extends Controller {

  def getGeneratedFacets(uid: String, entityType: String,lang: String) = Action { request =>
    val selectedFacets = if(request.body.asJson != null) request.body.asJson.get.as[Map[String,List[String]]] else null

    var subFilterQuery = ""
    if(selectedFacets != null){
        val predicates = selectedFacets.keys.mkString(" ")
        Logger.info("Facets for search : " + uid + " entityType: "+entityType + " body: "+ predicates)
        val selRdfs = selectedFacets.map { x =>
          """{
              {?s <%s> "%s" }
              UNION
              {
                ?s <%s> ?resource .
                ?resource fs:name "%s" .
                FILTER(isURI(?resource))
              }
            }""".format(x._1, x._2(0), x._1, x._2(0))
        }

      val  selRdfPatterns = selRdfs.mkString("\n")
        subFilterQuery = if (selRdfPatterns.length() > 0) s"""{
                                                                SELECT ?s
                                                                WHERE
                                                                {
                                                                    $selRdfPatterns
                                                                }
                                                              }""" else ""
    }
    else{
        Logger.info("Facets for search : " + uid + " entityType: "+entityType)
    }



    val typeEntity = entityType match {
      case "person" => "foaf:Person"
      case "organization" => "foaf:Organization"
      case "product" => "gr:ProductOrService"
      case "document" => "fs:Document"
      case "website" => "fs:Annotation"
    }

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>

        var queryModel = model
        Logger.info("Model size: "+queryModel.size())
        if(subFilterQuery != "") {
          queryModel = getGenericSubModel(model, subFilterQuery)
          Logger.info("After Filtering Model size: " + queryModel.size())
        }
        val query = s"""
                       PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                       PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                       PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                       PREFIX gr: <http://purl.org/goodrelations/v1#>
                       PREFIX owl: <http://www.w3.org/2002/07/owl#>

                       SELECT (SAMPLE(?p) AS ?facet) (SAMPLE(?title) AS ?label) (COUNT(?p) as ?elems)
                       WHERE
                       {
                         ?s a fs:SearchableEntity .
                         ?s a $typeEntity .
                         ?p a owl:DatatypeProperty .
                         ?s ?p ?o .
                         ?p rdfs:label ?title .
                         FILTER (lang(?title) = '$lang')
                       }
                       GROUP BY ?p ?title
                    """
          val results = QueryExecutionFactory.create(query, queryModel).execSelect()
          val facetModel = getGenericFacetsModel(results, typeEntity, queryModel)
          Ok(RDFUtil.modelToTripleString(facetModel, Lang.JSONLD))
      case None =>
        InternalServerError("The provided UID has not a model associated in the cache.")
    }
  }
  private def getGenericSubModel(model :Model,subFilterQuery :String ) : Model = {
    val query = s"""
                       PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                       PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                       PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                       PREFIX gr: <http://purl.org/goodrelations/v1#>

                       CONSTRUCT
                        {
                          ?s ?p ?o .
                          ?p ?prop ?moreData .
                          ?s ?metaProp ?value .
                          ?o fs:name ?label
                        }
                       WHERE
                       {
                         ?s ?p ?o .
                         $subFilterQuery
                          OPTIONAL { ?p ?prop ?moreData . }
                          OPTIONAL { ?o fs:name ?label .}
                       }
                    """
    Logger.info("Filter Model Query: \n" + query)
    val subModel = QueryExecutionFactory.create(query, model).execConstruct()
    subModel
  }
  private def getGenericFacetsModel(resultSet: ResultSet, entityType :String, kg : Model) : Model = {

    val facetsModel = ModelFactory.createDefaultModel()
    while(resultSet.hasNext) {
      val result = resultSet.next
      val facetUri = result.getResource("facet").getURI
      val count = result.getLiteral("elems").getString
      val label = result.getLiteral("label").getString
      var id = ""
      Logger.info("Uri: "+facetUri+ " Label: " + label)
      if (facetUri.split("#").length > 1)
        id = facetUri.split("#").last
      else
        id = facetUri.split("/").last

      val resource = facetsModel.createResource(FuhsenVocab.FACET_URI + id)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_NAME), id)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_LABEL), label)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_VALUE), facetUri)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_COUNT), count)

      getGenericFacetValues(facetUri, entityType, kg).map( r => resource.addProperty(facetsModel.createProperty(FuhsenVocab.HAS_FACET_VAL), r))
//      Logger.info("facetId: "+id+" FacetValueSize: "+results.size+"")
    }
    facetsModel
  }

  private def getGenericFacetValues(facet: String, entityType :String, model: Model) : List[String] = {

    val query = s"""
                      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                      PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                      PREFIX gr: <http://purl.org/goodrelations/v1#>

                      SELECT (SAMPLE(?fct) AS ?facet) (COUNT(?fct) as ?elems)
                      WHERE {
                          ?s a $entityType .
                          {
                          ?s <$facet> ?fct .
                          FILTER(isLiteral(?fct))
                          }
                          UNION
                          {
                            ?s <$facet> ?resource .
                            ?resource fs:name ?fct .
                            FILTER(isURI(?resource))
                          }
                      } GROUP BY ?fct
                    """
    val resultSet = QueryExecutionFactory.create(query, model).execSelect()
    resultSet.map { r =>
      if (r.get("facet") != null && r.get("facet").isLiteral)
        r.getLiteral("facet").getString.replace("^"," ")+"^"+r.getLiteral("elems").getString
      else
        ""
    }.filter( p => !p.isEmpty ).toList
  }

  //Construct did not work, I do not understand why. Temporally we are executing select queries //getFacetResultSet
  /*private def getSubModelWithFacet(facet: String, entityType :String, model :Model) : Model = {

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
                 |?p rdf:type fs:FacetValue .
                 |?p fs:count ?nelements .
                 |?p foaf:gender ?gender
                 |}
                 |WHERE {
                 |  ?p rdf:type foaf:Person .
                 |  ?p foaf:gender ?gender .
                 | { SELECT ?gender ( COUNT(?gender) as ?nelements ) { ?p foaf:gender ?gender } GROUP BY ?gender }
                 |  FILTER ( ?nelements > 0 )
                 |}
          """.stripMargin)
            val myModel = QueryExecutionFactory.create(query, model).execConstruct()
            myModel
        }
    }

  }
  */

  //Old hard-coded implementation to generate Facets
  /*

  def getFacets(uid: String, entityType: String) = Action { request =>
    Logger.info("Facets for search : " + uid + " entityType: "+entityType)

    val model = ModelFactory.createDefaultModel()
    var current_model = ModelFactory.createDefaultModel()

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        Logger.info("Model size: "+model.size())

        val typeEntity = entityType match {
          case "person" => "foaf:Person"
          case "organization" => "foaf:Organization"
          case "product" => "gr:ProductOrService"
          case "document" => "fs:Document"
          case "website" => "fs:Annotation"
        }

        val query = s"""
                       PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                       PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                       PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                       PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                       PREFIX gr: <http://purl.org/goodrelations/v1#>

                       CONSTRUCT   {
                        ?s ?p ?o .
                       }
                       WHERE {
                          ?s ?p ?o .
                       		?s rdf:type $typeEntity .
                       }
                    """
        current_model = QueryExecutionFactory.create(query, model).execConstruct()
      case None =>
        InternalServerError("The provided UID has not a model associated in the cache.")
    }

    entityType match {
      case "person" =>
        //Creating fs:Search resource
        if(current_model.contains(null, model.createProperty("http://xmlns.com/foaf/0.1/gender"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Gender").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "gender")
        }
        if(current_model.contains(null, model.createProperty("http://xmlns.com/foaf/0.1/birthday"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Birthday").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "birthday")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#location"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Location").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#occupation"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Occupation").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "occupation")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#placeLived"))) {
          model.createResource(FuhsenVocab.FACET_URI + "LiveIn").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "liveIn")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#workedAt"))) {
          model.createResource(FuhsenVocab.FACET_URI + "WorkAt").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "workAt")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#studiedAt"))) {
          model.createResource(FuhsenVocab.FACET_URI + "StudyAt").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "studyAt")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#source"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Source").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "source")
        }
      case "organization" =>
        //Creating fs:Search resource
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#location"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Location").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#country"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Country").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
        }
      case "product" =>
        //Creating fs:Search resource
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#price"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Price").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "price")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#country"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Country").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#location"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Location").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#condition"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Condition").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "condition")
        }
      case "document" =>
        //Creating fs:Search resource
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#country"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Country").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#language"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Language").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "language")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#filetype"))) {
          model.createResource(FuhsenVocab.FACET_URI + "FileType").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "filetype")
        }
      case "website" =>
        //Creating fs:Search resource
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#entity-type"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Person").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "person")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#entity-type"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Product").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "product")
        }
        if(current_model.contains(null, model.createProperty("http://vocab.lidakra.de/fuhsen#entity-type"))) {
          model.createResource(FuhsenVocab.FACET_URI + "Organization").addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "organization")
        }
      case _ =>
    }

    Ok(RDFUtil.modelToTripleString(model, Lang.JSONLD))

  }

  def getFacetValues(uid: String, facet: String, entityType :String) = Action { request =>
    Logger.info("Facets values for : " + facet + " uid: "+uid+" entityType: "+entityType)

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        Logger.info("Model size: "+model.size())
        val subModel = getFacetsModel(getFacetResultSet(facet, entityType, model))
        Logger.info("Facet SubModel size: "+subModel.size())
        Ok(RDFUtil.modelToTripleString(subModel, Lang.JSONLD))
      case None =>
        InternalServerError("Provided uid has not result model associated.")
    }

  }

  private def getFacetResultSet(facet: String, entityType :String, model :Model) : ResultSet = {

    entityType match {
      case "person" =>
        facet match {
          case "gender" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?gender) AS ?facet) (COUNT(?gender) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p foaf:gender ?gender .
                 |} GROUP BY ?gender ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "birthday" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?birthday ) AS ?facet) (COUNT(?birthday) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:birthday ?birthday .
                 |} GROUP BY ?birthday ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "location" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?location) AS ?facet) (COUNT(?location) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:location ?location .
                 |} GROUP BY ?location ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "occupation" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?occupation) AS ?facet) (COUNT(?occupation) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:occupation ?occupation .
                 |} GROUP BY ?occupation ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "liveIn" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:placeLived ?livedAt .
                 |    ?livedAt foaf:name ?name .
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "workAt" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:workedAt ?workedAt .
                 |    ?workedAt foaf:name ?name .
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "studyAt" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:studiedAt ?studyAt .
                 |    ?studyAt foaf:name ?name .
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "source" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?source) AS ?facet) (COUNT(?source) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:source ?source .
                 |} GROUP BY ?source ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
      case "product" =>
        facet match {
          case "price" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?price) AS ?facet) (COUNT(?price) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:priceLabel ?price
                 |} GROUP BY ?price ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "country" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?country) AS ?facet) (COUNT(?country) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:country ?country
                 |} GROUP BY ?country ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "location" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?location) AS ?facet) (COUNT(?location) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:location ?location
                 |} GROUP BY ?location ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "condition" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?condition) AS ?facet) (COUNT(?condition) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:condition ?condition
                 |} GROUP BY ?condition ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
      case "document" =>
        facet match {
          case "country" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?country) AS ?facet) (COUNT(?country) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Document .
                 |    ?p fs:country ?country
                 |} GROUP BY ?country ORDER BY DESC(?elems)
                  """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "language" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?language) AS ?facet) (COUNT(?language) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Document .
                 |    ?p fs:language ?language
                 |} GROUP BY ?language ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "filetype" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?filetype) AS ?facet) (COUNT(?filetype) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Document .
                 |    ?p fs:extension ?filetype
                 |} GROUP BY ?filetype ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
      case "website" =>
        facet match {
          case "person" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Annotation .
                 |    ?p fs:entity-type <https://schema.org/Person> .
                 |    ?p fs:entity-name ?name
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "organization" =>
            val query = QueryFactory.create(
              s"""
                 PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Annotation .
                 |    ?p fs:entity-type <https://schema.org/Organization> .
                 |    ?p fs:entity-name ?name
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "product" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?name) AS ?facet) (COUNT(?name) as ?elems)
                 |WHERE {
                 |		?p rdf:type fs:Annotation .
                 |    ?p fs:entity-type <https://schema.org/Product> .
                 |    ?p fs:entity-name ?name
                 |} GROUP BY ?name ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
      case "organization" =>
        facet match {
          case "country" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?country) AS ?facet) (COUNT(?country) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Organization .
                 |    ?p fs:country ?country
                 |} GROUP BY ?country ORDER BY DESC(?elems)
                  """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "location" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?location) AS ?facet) (COUNT(?location) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Organization .
                 |    ?p fs:location ?location .
                 |} GROUP BY ?location ORDER BY DESC(?elems)
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
    }
  }

  private def getFacetsModel(resultSet: ResultSet) : Model = {

    val facetsModel = ModelFactory.createDefaultModel()
    var addBlank = false
    while(resultSet.hasNext) {
      val result = resultSet.next
      val name = result.getLiteral("facet").getString
      val count = result.getLiteral("elems").getString
      val resource = facetsModel.createResource(FuhsenVocab.FACET_URI + name.trim.replace(" ", ""))
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_VALUE), name)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_COUNT), count)
      addBlank = true
    }
    //Temporal solution since @graph does not appear when it is just one element
    if (addBlank) {
      val resource = facetsModel.createResource(FuhsenVocab.FACET_URI +"blankNode")
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_VALUE), "blank")
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_COUNT), "0")
    }

    facetsModel

  }

  */

}
