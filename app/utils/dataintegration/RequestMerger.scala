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

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{Dataset, QueryExecutionFactory, QueryFactory, Syntax}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang

/**
  * Created by andreas on 2/26/16.
  */
class RequestMerger() {
  private lazy val mergedModel: Model = ModelFactory.createDefaultModel()

  /**
    * Add the RDF data from one source
    *
    * @param model The RDF data to be added.
    * @param sourceGraphUri The unique source graph URI, this should always be the same for each unique source,
    *                       e.g. Google+, Twitter etc.
    */
  def addWrapperResult(model: Model, sourceGraphUri: String): Unit = {
    mergedModel.add(model)
    val provQuery = QueryFactory.create(
      s"""
        |CONSTRUCT {
        |  ?s <${FuhsenVocab.sourceUri}> <$sourceGraphUri>
        |} WHERE {
        |  ?s ?p ?o
        |}
      """.stripMargin)
    val provModel = QueryExecutionFactory.create(provQuery, model).execConstruct()
    mergedModel.add(provModel)
  }

  def addLinks(model: Model): Unit ={
    mergedModel.add(model)
  }

  def constructQuadDataset(): Dataset = {
    val quadQuery = QueryFactory.create(
      s"""
        |CONSTRUCT {
        |  GRAPH ?sourceGraph {
        |    ?s ?p ?o
        |  }
        |} WHERE {
        |  ?s ?p ?o .
        |  ?s <${FuhsenVocab.sourceUri}> ?sourceGraph .
        |}
      """.stripMargin,
      Syntax.syntaxARQ
    )
    val quadDataset = QueryExecutionFactory.create(quadQuery, mergedModel).execConstructDataset()
    quadDataset
  }

  def serializeMergedModel(lang: Lang): String = {
    RDFUtil.modelToTripleString(mergedModel, lang)
  }
}
