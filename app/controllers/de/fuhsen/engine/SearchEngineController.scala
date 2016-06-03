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

package controllers.de.fuhsen.engine

import java.util.{Calendar, UUID}
import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.riot.Lang
import utils.dataintegration.RDFUtil
import scala.concurrent.Future
import javax.inject.Inject
import org.apache.jena.rdf.model.ModelFactory
import play.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class SearchEngineController @Inject()(ws: WSClient) extends Controller {

  def search(query: String) = Action.async {

    Logger.info("Starting Search Engine Search with query: " + query)

    val searchUid = UUID.randomUUID.toString()
    val searchURI = FuhsenVocab.SEARCH_URI + searchUid

    val model = ModelFactory.createDefaultModel()

    //Creating fs:Search resource
    model.createResource(searchURI)
          .addProperty(model.createProperty(FuhsenVocab.UID), searchUid)
          .addProperty(model.createProperty(FuhsenVocab.KEYWORD), query)
          .addProperty(model.createProperty(FuhsenVocab.QUERY_DATE), Calendar.getInstance.getTime.toString)

    val data = RDFUtil.modelToTripleString(model, Lang.TURTLE)

    //Micro-task services executed
    val futureResponse: Future[WSResponse] = for {
      responseOne <- ws.url("http://localhost:9000/engine/api/queryprocessing?query=Diego").post(data)
      responseTwo <- ws.url("http://localhost:9000/engine/api/federatedquery?query=Diego").post(responseOne.body)
      responseThree <- ws.url("http://localhost:9000/engine/api/entitysummarization?query=Diego").post(responseTwo.body)
      responseFour <- ws.url("http://localhost:9000/engine/api/semanticranking?query=Diego").post(responseThree.body)
    } yield responseFour

    futureResponse.recover {
      case e: Exception =>
        val exceptionData = Map("error" -> Seq(e.getMessage))
        //ws.url(exceptionUrl).post(exceptionData)
        Logger.error(exceptionData.toString())
    }

    futureResponse.map (
      r => Ok(r.body)
    )

  }

}
