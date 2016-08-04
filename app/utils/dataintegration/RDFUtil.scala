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

import java.io._
import org.apache.jena.query.Dataset
import org.apache.jena.rdf.model.{Model, ModelFactory, Statement, StmtIterator}
import org.apache.jena.graph.Triple
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFLanguages}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

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
    lang.toString match {
      case csv if csv.toString == "Lang:CSV" => modelToCSV(model)
      case _ =>
        val output = new StringWriter()
        RDFDataMgr.write(output, model, lang)
        output.toString()
    }
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
    ModelFactory.createDefaultModel().read(new ByteArrayInputStream(body.getBytes("UTF-8")), null, lang.getName)
  }

  def rdfReaderToModel(reader: Reader, lang: Lang): Model = {
    ModelFactory.createDefaultModel().read(reader, null, lang.getName)
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

  def modelToCSV(model: Model): String = {
    val statements : StmtIterator = model.listStatements()
    var csv_pre_rows = new ListBuffer[Person_Helper]()
    while(statements.hasNext) {
      val current_stm : Statement = statements.nextStatement
      csv_pre_rows.find(curr_person => current_stm.getSubject.toString.contains(curr_person.id)) match {
        case matched_person : Some[Person_Helper] =>
          populatePerson(current_stm.getPredicate.toString, matched_person.get, current_stm)
        case None => //It is a new person.
          val temp_helper = current_stm.getSubject.toString.split("/")
          var new_person_helper = new Person_Helper(temp_helper(temp_helper.size - 1), "", "", "", "")
          populatePerson(current_stm.getPredicate.toString, new_person_helper, current_stm)
          csv_pre_rows += new_person_helper
      }
    }
    var result_str = ""
    csv_pre_rows.map( current =>
      result_str += current.id+";"+current.name+";"+current.family_name+";"+current.given_name+";"+current.img+sys.props("line.separator")
    )
    result_str
  }

  def populatePerson(property:String, person: Person_Helper, current_stm : Statement) : Unit ={
      property match {
        case a if a == "http://xmlns.com/foaf/0.1/name" => person.name = current_stm.getObject.toString
        case b if b == "http://xmlns.com/foaf/0.1/family_name" => person.family_name = current_stm.getObject.toString
        case c if c == "http://xmlns.com/foaf/0.1/givenname" => person.given_name = current_stm.getObject.toString
        case d if d == "http://xmlns.com/foaf/0.1/img" => person.img = current_stm.getObject.toString
        case __ =>
        }
  }
}

case class Person_Helper(var id: String, var name: String, var family_name: String, var given_name: String, var img: String)