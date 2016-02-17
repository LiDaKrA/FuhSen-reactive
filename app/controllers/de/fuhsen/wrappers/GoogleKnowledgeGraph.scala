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
 
package controllers.de.fuhsen.wrappers

import javax.inject.Inject
import com.typesafe.config.ConfigFactory
import play.Logger
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.xml.Elem

class GoogleKnowledgeGraph @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Google Knowledge Graph Search with query: " + query)

    val KEY = ConfigFactory.load.getString("gkb.app.key")

    val url = ConfigFactory.load.getString("gkb.url")
    val apiRequest: WSRequest = ws.url(url)
      .withQueryString("query" -> query)
      .withQueryString("key" -> KEY)
      .withQueryString("types" -> "Person")

    //Preparing Transformation task call
    val transformTaskUrl = ConfigFactory.load.getString("silk.server.url") + ConfigFactory.load.getString("gkb.transform.url")
    val transformRequest: WSRequest = ws.url(transformTaskUrl)
      .withHeaders("Content-Type" -> "application/xml")
      .withHeaders("Accept" -> "application/ld+json")

    for {
      responseApi <- apiRequest.get()
      data = dataTransformationBody(responseApi.body)
      responseTransformation <- transformRequest.post(data)
    } yield {
      Ok(responseTransformation.body)
    }

    /*request.get().map { response =>
      Ok(response.body)
    }*/

  }

  private def dataTransformationBody (content: String) : Elem = {
    val data = <Transform>
      <DataSources>
        <Dataset id="GoogleKB_Entities">
          <DatasetPlugin type="json">
            <Param name="file" value="gkb"/>
            <Param name="basePath" value="itemListElement"/>
            <Param name="uriPattern" value="http://vocab.cs.uni-bonn.de/fuhsen/search/entity/gkb/{id}"/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="gkb">{content}
      </resource>
    </Transform>
    data
  }

}
