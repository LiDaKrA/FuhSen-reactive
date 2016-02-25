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
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import scala.xml.Elem
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Tor2Web @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Tor2Web Search with query: " + query)

    val KEY = ConfigFactory.load.getString("tor2web.app.key")

    val url = ConfigFactory.load.getString("tor2web.url")
    val apiRequest: WSRequest = ws.url(url)
      .withQueryString("q" -> query)
      .withQueryString("key" -> KEY)

    val TRANSFORMTASKURL = ConfigFactory.load.getString("silk.server.url") + ConfigFactory.load.getString("tor2web.transform.url")
    val transformRequest: WSRequest = ws.url(TRANSFORMTASKURL)
      .withHeaders("Content-Type" -> "application/xml")
      .withHeaders("Accept" -> "application/ld+json")

    for {
      responseApi <- apiRequest.get()
      data = dataTranformationBody(responseApi.body)
      responseTransformation <- transformRequest.post(data)
    } yield {
      Ok(responseTransformation.body)
    }
    /*apiRequest.get().map { response =>
      Ok(response.body)
    }*/
  }

  private def dataTranformationBody (content: String) : Elem = {
    val data = <Transform>
      <DataSources>
        <Dataset id="DarkSites">
          <DatasetPlugin type="json">
            <Param name="file" value="torSites"/>
            <Param name="basePath" value="doc"/>
            <Param name="uriPattern" value="http://vocab.cs.uni-bonn.de/fuhsen/search/entity/tor2web/{@attributes/docId}"/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="torSites">{content}
      </resource>
    </Transform>
    data
  }

}