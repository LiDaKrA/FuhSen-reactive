package controllers.de.fuhsen.visualization

import java.io.{BufferedWriter, FileWriter}
import java.util.logging.Logger

import org.apache.jena.rdf.model._
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.{RDF, RDFS}
import play.api.libs.json._
import utils.dataintegration.RDFUtil

import scala.collection.JavaConverters._
import scala.collection.{Seq, mutable}
import scala.io.Source

/**
  * A converter that turns RDF instance data into the VOWL-JSON format
  */
object Instance2VOWLJSON {
  val log: Logger = Logger.getLogger(this.getClass.getName)

  // URI for tracking provenance of a resource, only provenance property that is present on all resources not just root resources
  val sourceUriProperty = "http://vocab.lidakra.de/fuhsen#merge/sourceUri"
  // Properties that should be ignored in the visualization
  val propertyBlacklist: Set[String] = Set[String](
    RDF.`type`.getURI, // Else entities will link/cluster only because of the same type
    "http://vocab.lidakra.de/fuhsen#merge/sourceUri", // same here, same source is not specific enough
    "http://vocab.lidakra.de/fuhsen#url", // same here
    "http://vocab.lidakra.de/fuhsen#source", // same here
    RDFS.label.getURI
  )
  // Classes that should be ignored in the visualization
  val classBlacklist: Set[String] = Set[String](
    "http://www.w3.org/ns/prov#Activity" // this is meta data which should not be displayed
  )

  /** Turn Jena Model into VOWL-JSON */
  def convertModelToJson(model: Model): JsObject = {
    val idGenerator = IdGenerator()
    val (literalNodes: List[LiteralNode], propertyNodes: List[VowlProperty]) = extractVowlItemsFromModel(model, idGenerator)
    val instanceNodes = idGenerator.instanceNodes.toSeq
    val instanceNodesJson = generateClassJson(instanceNodes, literalNodes)
    val instanceNodeAttributesJson = generateClassAttributeJson(instanceNodes, literalNodes)
    val propertyJson = generatePropertyJson(propertyNodes)
    val propertyAttributeJson = generatePropertyAttributeJson(propertyNodes)

    val result = Json.obj(// Metrics, that must be defined, but are ignored for our use case, values must be > 0 for some metrics
      "namespace" -> JsArray(),
      "metrics" -> Json.obj(
        "classCount" -> JsNumber(1),
        "objectPropertyCount" -> JsNumber(1),
        "datatypePropertyCount" -> JsNumber(1),
        "individualCount" -> JsNumber(0)
      ),
      "class" -> JsArray(instanceNodesJson),
      "classAttribute" -> JsArray(instanceNodeAttributesJson),
      "property" -> JsArray(propertyJson),
      "propertyAttribute" -> JsArray(propertyAttributeJson)
    )
    result
  }

  def convertModelToJsonString(model: Model): String = {
    convertModelToJson(model).toString
  }

  /** Extracts instances, properties and literals from Jena Model. Instances are stored in the idGenerator object. */
  private def extractVowlItemsFromModel(model: Model, idGenerator: IdGenerator): (List[LiteralNode], List[VowlProperty]) = {
    val sourceUri = model.getProperty(sourceUriProperty)
    val instances = model.listResourcesWithProperty(sourceUri)
    var literalNodes = List.empty[LiteralNode]
    var propertyNodes = List.empty[VowlProperty]
    var counter = 0
    while (instances.hasNext && counter < 10) {
      val instance = instances.nextResource()
      val types = propertyObjects(instance, RDF.`type`)
      val provString: Option[String] = extractProvenance(sourceUri, instance)
      if (types.forall(t => !t.isURIResource || !classBlacklist.contains(t.asResource().getURI))) {
        val instanceId = idGenerator.uri2Id(instance)
        val (ps, ls) = extractVowlProperties(instanceId, idGenerator, literalNodes, instance, provString)
        propertyNodes :::= ps
        literalNodes :::= ls
      }
      counter += 1
    }
    (literalNodes, propertyNodes)
  }

  // Extract the provenance and return it as string that can be appended to property labels and literal values
  private def extractProvenance(sourceUri: Property, instance: Resource): Option[String] = {
    val source = propertyObjects(instance, sourceUri)
    val provString = source.headOption.map { s =>
      " (" + extractLocalName(s.asResource().getURI) + ")"
    }
    provString
  }

  // Fetch the object nodes of the instance for a specific property
  private def propertyObjects(instance: Resource, propertyUri: String): Seq[RDFNode] = {
    instance.listProperties(instance.getModel.getProperty(propertyUri)).asScala.toSeq.map(_.getObject)
  }

  private def propertyObjects(instance: Resource, property: Property): Seq[RDFNode] = {
    propertyObjects(instance, property.getURI)
  }

  private def extractVowlProperties(instanceId: Int,
                                    idGenerator: IdGenerator,
                                    literalNodes: List[LiteralNode],
                                    instance: Resource,
                                    provString: Option[String]): (List[VowlProperty], List[LiteralNode]) = {
    val allPropertyStmts = instance.listProperties()
    var propertyNodes = List.empty[VowlProperty]
    var literalNodes = List.empty[LiteralNode]
    while (allPropertyStmts.hasNext) {
      val propStmt = allPropertyStmts.next()
      val prop = propStmt.getPredicate
      if (!propertyBlacklist.contains(prop.getURI)) {
        val objectIdOpt = propStmt.getObject match {
          case r: Resource =>
            Some((idGenerator.uri2Id(r), ObjectPropertyType))
          case l: Literal =>
            val literalValue = l.getValue match {
              case d: java.lang.Double if d < 1 && d > 0.0001 =>
                d.formatted("%f.5")
              case f: java.lang.Float if f < 1 && f > 0.0001 =>
                f.formatted("%f.5")
              case d: java.lang.Double if d >= 1 =>
                fmt(d)
              case f: java.lang.Float if f >= 1 =>
                fmt(f.toDouble)
              case _ =>
                l.getLexicalForm
            }
            val literalId = idGenerator.nextId()
            val literalNode = LiteralNode(literalId, literalValue + provString.getOrElse(""))
            literalNodes ::= literalNode
            Some((literalId, LiteralPropertyType))
          case other: RDFNode =>
            // Ignore everything else
            log.fine("Got object class of " + other.getClass.getSimpleName + " for entity " + instance.getURI)
            None
        }
        objectIdOpt foreach { case (objId, typ) =>
          val vowlProperty = VowlProperty(
            id = idGenerator.nextId(),
            label = prop.getLocalName + (if (typ == ObjectPropertyType) provString.getOrElse("") else ""),
            domain = instanceId,
            range = objId,
            propertyType = typ
          )
          propertyNodes ::= vowlProperty
        }
      }
    }
    (propertyNodes, literalNodes)
  }

  private def generatePropertyAttributeJson(propertyNodes: List[VowlProperty]) = {
    propertyNodes map { prop =>
      Json.obj(
        "id" -> JsString(prop.id.toString),
        "label" -> jsonLabel(prop.label),
        "domain" -> JsString(prop.domain.toString),
        "range" -> JsString(prop.range.toString)
      )
    }
  }

  private def generatePropertyJson(propertyNodes: List[VowlProperty]) = {
    propertyNodes map { prop =>
      val typeString = prop.propertyType match {
        case ObjectPropertyType => "owl:objectProperty"
        case LiteralPropertyType => "owl:datatypeProperty"
      }
      Json.obj(
        "id" -> JsString(prop.id.toString),
        "type" -> JsString(typeString)
      )
    }
  }

  private def fmt(d: Double): String = {
    if(d == d.toLong)
      d.toLong.formatted("%d")
    else
      d.formatted("%s")
  }

  private def generateClassAttributeJson(instanceNodes: Seq[InstanceNode], literalNodes: Seq[LiteralNode]): Seq[JsObject] = {
    (instanceNodes ++ literalNodes) map { instanceNode =>
      Json.obj(
        "id" -> JsString(instanceNode.id.toString),
        "label" -> jsonLabel(instanceNode.label)
      )
    }
  }

  private def generateClassJson(instanceNodes: Seq[InstanceNode], literalNodes: List[LiteralNode]): Seq[JsObject] = {
    val instances = instanceNodes map { instanceNode =>
      Json.obj(
        "id" -> JsString(instanceNode.id.toString),
        "type" -> JsString("owl:Class")
      )
    }
    val literals = literalNodes map { instanceNode =>
      Json.obj(
        "id" -> JsString(instanceNode.id.toString),
        "type" -> JsString("owl:Thing") // FIXME: Change to rdfs:Literal when labelling is fixed
      )
    }
    instances ++ literals
  }

  def singleOpt(resource: Resource, prop: Property): Option[RDFNode] = {
    val it = resource.listProperties(prop)
    if (it.hasNext) {
      Some(it.next().getObject)
    } else {
      None
    }
  }

  def jsonLabel(label: String): JsValue = {
    Json.obj(
      "undefined" -> label
    )
  }

  def extractLocalName(uri: String): String = {
    val idx = math.max(math.max(uri.lastIndexOf('#') + 1, uri.lastIndexOf('/')) + 1, 0)
    val localName = uri.substring(idx)
    localName
  }
}

case class IdGenerator() {
  val log: Logger = Logger.getLogger(this.getClass.getName)
  private var idCounter = 0
  private val uri2IdMap = mutable.HashMap.empty[String, Int]
  private val instNodes = mutable.HashSet.empty[InstanceNode]

  def instanceNodes: Set[InstanceNode] = instNodes.toSet

  def uri2Id(instance: Resource): Int = {
    val uri = instance.getURI
    val instanceId = uri2IdMap.getOrElseUpdate(uri, nextId())
    Instance2VOWLJSON.singleOpt(instance, RDFS.label) match {
      case Some(rdfNode) =>
        instNodes.add(InstanceNode(instanceId, rdfNode.asLiteral().getString))
      case None =>
        val localName: String = Instance2VOWLJSON.extractLocalName(uri)
        instNodes.add(InstanceNode(instanceId, localName))
        log.fine("No label found for instance <" + instance.getURI + ">!")
    }
    instanceId
  }

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }
}

sealed trait VowlNode {
  def label: String

  def id: Int
}

case class InstanceNode(id: Int, label: String) extends VowlNode

case class LiteralNode(id: Int, label: String) extends VowlNode

case class VowlProperty(id: Int, label: String, domain: Int, range: Int, propertyType: PropertyType)

sealed trait PropertyType

case object LiteralPropertyType extends PropertyType

case object ObjectPropertyType extends PropertyType

object Test {
  // Turns FuhSen RDF instance result data into VOWL-JSON graph
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println("Parameters: <In turtle> <Out VOWL-JSON>")
      System.exit(1)
    }
    val turtleFile = args(0)
    val model = RDFUtil.rdfStringToModel(Source.fromFile(turtleFile).mkString, Lang.TTL)
    val result = Instance2VOWLJSON.convertModelToJsonString(model)
    val out = new BufferedWriter(new FileWriter(args(1)))
    out.append(result)
    out.flush()
    out.close()
  }

}