/*
 * Copyright (C) 2016 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils.dataintegration

import java.io.{ByteArrayInputStream, StringWriter}

import org.apache.jena.query.Dataset
import org.apache.jena.rdf.model.{ModelFactory, Model}
import org.apache.jena.graph.Triple
import org.apache.jena.riot.{RDFLanguages, Lang, RDFDataMgr}

import scala.collection.mutable.ArrayBuffer

/**
  * Created on 2/29/16.
  */
object RDFUtil {
  def datasetToQuadString(dataset: Dataset,
                          lang: Lang = Lang.NQUADS): String = {
    val output = new StringWriter()
    RDFDataMgr.write(output, dataset, lang)
    output.toString()
  }

  def modelToTripleString(model: Model,
                          lang: Lang): String = {
    val output = new StringWriter()
    RDFDataMgr.write(output, model, lang)
    output.toString()
  }

  def stringToTriple(rdfContent: String, lang: Lang): Traversable[Triple] = {
    val model = rdfStringToModel(rdfContent, lang.getName)
    val it = model.listStatements()
    val triples = ArrayBuffer.empty[Triple]
    while(it.hasNext) {
      val statement = it.nextStatement()
      triples.append(statement.asTriple())
    }
    triples
  }

  def rdfStringToModel(body: String, lang: Lang): Model = {
    if(body == "") {
      return ModelFactory.createDefaultModel()
    }
    ModelFactory.createDefaultModel().read(new ByteArrayInputStream(body.getBytes()), null, lang.getName)
  }

  /** Returns the Jena RDF Lang name for an accept type. Defaults to Turtle */
  def acceptTypeToRdfLang(acceptType: String): String = {
    Option(RDFLanguages.contentTypeToLang(acceptType)).
        getOrElse(Lang.TURTLE).
        getName
  }

  def langToAcceptType(lang: Lang): String = {
    lang.getContentType.getContentType
  }

  def langNameToLang(langName: String): Lang = {
    RDFLanguages.nameToLang(langName)
  }

  implicit def stringToLang(langName: String): Lang = {
    langNameToLang(langName)
  }
}
