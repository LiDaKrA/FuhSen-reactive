package utils.dataintegration

import java.util.logging.Logger

import org.apache.jena.graph.{Triple, Node, NodeFactory}
import org.apache.jena.sparql.core.Quad
import org.apache.jena.vocabulary.OWL

import scala.collection.mutable
import scala.util.control.Breaks._


/**
  * Created on 2/29/16.
  */
object UriTranslator {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * Rewrite the URIs of the entities in subject and object position to the global entity URI of each
    * sameAs cluster, i.e. of each strongly connected component after doing the transitive closure over
    * the sameAs links.
    *
    * @param quads The input quads
    * @param uriMap The map from entity URI to global entity URI from the same sameAs component
    * @return
    */
  def rewriteURIs(quads: Traversable[Quad])
                 (implicit uriMap: Map[String, String]): Traversable[Quad] = {
    var quadCounter = 0
    var sameAsCounter = 0
    val entityGraphChecker = new EntityGraphChecker

    log.info("Start quad URI translation.")
    val translatedQuads = for (quad <- quads) yield {
      quadCounter += 1
      val links = checkAndWriteSameAsLinks(entityGraphChecker, quad.getGraph.getURI, quad.getSubject, quad.getObject)
      val (sNew, oNew) = translateQuadURIs(quad.getSubject, quad.getObject)
      val rewrittenQuad = new Quad(quad.getGraph, sNew, quad.getPredicate, oNew)
      sameAsCounter += links.size
      rewrittenQuad +: links
    }
    log.info(s"End of URI translation: Rewrote $quadCounter quads and output $sameAsCounter sameAs links.")
    translatedQuads.flatten
  }

  private def translateNode(node: Node)
                       (implicit uriMap: Map[String, String]): Node = {
    if (node.isURI)
      translateURINode(node)
    else
      node
  }

  private def translateQuadURIs(subject: Node,
                                obj: Node)
                               (implicit uriMap: Map[String, String]): (Node, Node) = {
    (translateNode(subject), translateNode(obj))
  }

  def translateQuads(quads: Traversable[Quad],
                     links: Traversable[Triple]): Traversable[Quad] = {
    implicit val uriMap: Map[String, String] = generateUriMap(links)
    rewriteURIs(quads)
  }

  /** Generate sameAs links to global entity if not already done */
  private def checkAndWriteSameAsLinks(entityGraphChecker: EntityGraphChecker,
                                       graph: String,
                                       nodes: Node*)
                                      (implicit uriMap: Map[String, String]): Seq[Quad] = {
    for (node <- nodes if node.isURI;
         globalUri <- uriMap.get(node.getURI)
         if (entityGraphChecker.addAndCheck(node.getURI, graph))) yield {
      new Quad(
        NodeFactory.createURI(graph),
        node,
        NodeFactory.createURI(OWL.sameAs.getURI),
        NodeFactory.createURI(globalUri))
    }
  }

  /** Returns translated URI if they are found in the map, else returns the original URI */
  private def translateURINode(uriNode: Node)(implicit uriMap: Map[String, String]): Node = {
    if (uriMap.contains(uriNode.getURI))
      NodeFactory.createURI(uriMap.get(uriNode.getURI).get)
    else
      uriNode
  }

  /** Generate a Map from uri to "global" uri */
  def generateUriMap(sameAsStmts: Traversable[Triple]): Map[String, String] = {
    val entityToClusterMap: Map[String, EntityCluster] = createEntityCluster(sameAsStmts)
    generateUriMap(entityToClusterMap)
  }

  private def generateUriMap(entityToClusterMap: Map[String, EntityCluster]): Map[String, String] = {
    val uriMap = new mutable.HashMap[String, String]()

    for ((fromURI, toURICluster) <- entityToClusterMap) {
      val toURI = toURICluster.getGlobalEntity
      if (fromURI != toURI)
        uriMap.put(fromURI, toURI)
    }

    uriMap.toMap
  }

  /** Create entity cluster, i.e. transitive closures of the sameAs link graph */
  private def createEntityCluster(sameAsStmts: Traversable[Triple]): Map[String, EntityCluster] = {
    val entityToClusterMap = new mutable.HashMap[String, EntityCluster]()

    //    val overAllCount = linkReader.size
    var counter = 0

    for (quad <- sameAsStmts) {
      counter += 1
      val (entity1, entity2) = extractEntityStrings(quad)
      val clusterOfEntity1 = entityToClusterMap.get(entity1)
      val clusterOfEntity2 = entityToClusterMap.get(entity2)

      (clusterOfEntity1, clusterOfEntity2) match {
        case (None, None) => {
          val cluster = new EntityCluster(entity1)
          entityToClusterMap.put(entity1, cluster)
          cluster.integrateEntity(entity2, entityToClusterMap)
        }
        case (None, Some(cluster2)) => cluster2.integrateEntity(entity1, entityToClusterMap)
        case (Some(cluster1), None) => cluster1.integrateEntity(entity2, entityToClusterMap)
        case (Some(cluster1), Some(cluster2)) => {
          val globalCluster1 = cluster1.getGlobalCluster
          if (globalCluster1 != cluster2.getGlobalCluster)
            globalCluster1.integrateCluster(cluster2.getGlobalCluster(), entityToClusterMap)
        }
      }
    }

    entityToClusterMap.toMap
  }

  private def extractEntityStrings(triple: Triple): (String, String) = (triple.getSubject.getURI, triple.getObject.getURI)
}

/**
  * A data structure to efficiently store and construct transitive sameAs relationships of a subset of entities.
  * Also used to choose a global URI (representative) for each entity cluster.
  *
  * @param entity
  * @param entitySet
  */
case class EntityCluster(var entity: String,
                         entitySet: mutable.HashSet[String]) {

  var parentCluster: EntityCluster = null

  def this(entity: String) = this(entity, mutable.HashSet(entity))

  def addEntities(entities: Traversable[String]) = {
    entitySet ++= entities
  }

  /** Moves all the entities to this cluster, making the other cluster obsolete */
  def integrateCluster(other: EntityCluster, entityToClusterMap: mutable.Map[String, EntityCluster]) {
    if (entityComparator.lessThan(other.entity, entity))
      other.parentCluster = this
    else
      parentCluster = other
  }

  /** Either adds the new entity to this cluster or creates a new cluster with the new entity which becomes the parent. */
  def integrateEntity(newEntity: String, entityToClusterMap: mutable.Map[String, EntityCluster]) {
    if (entityComparator.lessThan(newEntity, entity)) {
      entitySet += newEntity
      entityToClusterMap.put(newEntity, this)
    }
    else {
      parentCluster match {
        case null =>
          val newCluster = new EntityCluster(newEntity)
          entityToClusterMap.put(newEntity, newCluster)
          parentCluster = newCluster
        case _ =>
          parentCluster.integrateEntity(newEntity, entityToClusterMap)
      }
    }
  }

  /** The entity from the top-parent cluster will be the global entity URI */
  def getGlobalEntity(): String = {
    if (isGlobalCluster()) {
      entity
    } else {
      parentCluster.getGlobalEntity
    }
  }

  def isGlobalCluster(): Boolean = {
    parentCluster == null
  }

  def setGlobalEntity(uri: String) {
    if (isGlobalCluster()) {
      entity = uri
    } else {
      parentCluster.setGlobalEntity(uri)
    }
  }

  /** Update the current global URI iff this one is "larger" */
  def setGlobalEntityIfLarger(uri: String) {
    if (isGlobalCluster()) {
      if (uri > entity) {
        entity = uri
      }
    } else {
      parentCluster.setGlobalEntityIfLarger(uri)
    }
  }

  def getGlobalCluster(): EntityCluster = {
    if (isGlobalCluster()) {
      this
    } else {
      parentCluster.getGlobalCluster
    }
  }

  def size = entitySet.size
}

class EntityGraphChecker {
  val entityGraphMap = new mutable.HashMap[String, mutable.Set[String]]()

  /**
    * Add the graph to the entity.
    *
    * @return true if the graph has not been added before, else false
    */
  def addAndCheck(entity: String, graph: String): Boolean = {
    val entityGraphSet = entityGraphMap.getOrElseUpdate(entity, new mutable.HashSet[String])
    if (entityGraphSet.contains(graph))
      return false
    entityGraphSet.add(graph)
    true
  }
}

object entityComparator {

  private var prefixPreference: Seq[String] = Seq()

  /**
    * Count how many of the characters in a String are letters (of any language)
    * Used as heuristic to measure how "readable" a String is.
    */
  def countAlphaNumChars(string: String) = {
    var nLetters = 0
    val nChars = string.length()
    for (i <- 0 until nChars) {
      if (Character.isLetter(string.charAt(i)))
        nLetters = nLetters + 1
    }
    nLetters.toDouble / nChars
  }

  def setPrefixPreference(prefixPref: Seq[String]) {
    prefixPreference = prefixPref
  }

  /**
    * Count how many of the characters in a String are "non-legit" characters (not [a-zA-Z()]
    * Used as heuristic to measure how "readable" a String is to Anglo-Saxonic and Latin eyes.
    */
  def countNonLegitChars(string: String) = {
    var nLetters = 0
    val nChars = string.length()
    for (i <- 0 until nChars) {
      val char = string.charAt(i)
      if (!(char >= 'a' && char <= 'z' || char >= 'A' && char <= 'Z' || char == '(' || char == ')'))
        nLetters = nLetters + 1
    }
    nLetters
  }

  /**
    * This function is used to decide when String "left" is considered "smaller" (worse) than the String "right".
    * First they are compared based on countNonLegitChars. If they are the same there use lexicographic oder.
    * +   * If prefixPreference is set through property "prefixPreference",
    */
  def lessThan(left: String, right: String): Boolean = {
    breakable {
      for (prefix <- prefixPreference) {
        val leftTrue = left.startsWith(prefix)
        val rightTrue = right.startsWith(prefix)
        if (!leftTrue && rightTrue)
          return true
        else if (leftTrue && !rightTrue)
          return false
        else if (leftTrue && rightTrue)
          break
      }
    }
    val leftCount = countNonLegitChars(left)
    val rightCount = countNonLegitChars(right)
    if (leftCount == rightCount)
      left < right
    else
      leftCount > rightCount
  }
}