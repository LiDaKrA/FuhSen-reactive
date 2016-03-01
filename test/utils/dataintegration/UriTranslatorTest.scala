package utils.dataintegration

import org.apache.jena.graph.NodeFactory
import org.apache.jena.sparql.core.Quad
import org.apache.jena.vocabulary.OWL
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created on 2/29/16.
  */
class UriTranslatorTest extends FlatSpec with Matchers {
  behavior of "UriTranslator"

  val sameAsStatements = Seq(
    quad("s1", "sG"),
    quad("s3", "sG"),
    quad("s5", "s3"),
    quad("s8", "s3"),
    quad("sA", "sB"),
    quad("sD", "sB"),
    quad("sC", "sA")
  ) map (_.asTriple())

  implicit val uriCluster = UriTranslator.generateUriMap(sameAsStatements)

  it should "create correct transitive closures of sameAs graphs" in {
    uriCluster("s1") shouldBe "sG"
    uriCluster("s3") shouldBe "sG"
    uriCluster("s5") shouldBe "sG"
    uriCluster("s8") shouldBe "sG"
    uriCluster.get("sG") shouldBe None
    uriCluster("sA") shouldBe uriCluster("sB")
    uriCluster("sC") shouldBe uriCluster("sB")
    uriCluster("sA") shouldBe "sD"
  }

  it should "rewrite URIs in quads" in {
    val rewrittenQuads = UriTranslator.rewriteURIs(Seq(
      quad(s = "s1", o = "sC", g = "g1", p = "p1"),
      quad(s = "s3", o = "sA", g = "g3", p = "p2"),
      quad(s = "s1", o = "sB", g = "g1", p = "p3")
    ))
    rewrittenQuads.size shouldBe 8
    rewrittenQuads.filter(_.getPredicate.getURI.contains("sameAs")).size shouldBe 5
  }

  private def quad(s: String, o: String, g: String = "default", p: String = OWL.sameAs.getURI): Quad = {
    new Quad(
      NodeFactory.createURI(g),
      NodeFactory.createURI(s),
      NodeFactory.createURI(p),
      NodeFactory.createURI(o)
    )
  }
}
