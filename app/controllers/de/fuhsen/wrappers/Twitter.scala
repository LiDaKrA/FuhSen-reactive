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
import play.api.libs.oauth.{OAuthCalculator, ConsumerKey, RequestToken}
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.xml.Elem

class Twitter @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Twitter Search with query:  " + query)

    //Preparing Twitter API call
    val KEY = ConsumerKey(ConfigFactory.load.getString("twitter.consumer.key"), ConfigFactory.load.getString("twitter.consumer.secret"))
    val TOKEN: RequestToken = new RequestToken(ConfigFactory.load.getString("twitter.access.token"), ConfigFactory.load.getString("twitter.access.secret"))
    var query_string: String = query.replace(" ", "%20")
    Logger.info("Query String Transformed: " + query_string)

    val url = ConfigFactory.load.getString("twitter.url")
    val apiRequest: WSRequest = ws.url(url)
                                  .withQueryString("q" -> query_string)
                                  .sign(OAuthCalculator(KEY, TOKEN))

    //Preparing Transformation task call
    val TRANSFORMTASKURL = ConfigFactory.load.getString("silk.server.url") + ConfigFactory.load.getString("twitter.transform.url")
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

    /*request.get().map { response =>
      Ok(response.body)
    }*/

  }

  private def dataTranformationBody (cotent: String) : Elem = {
    val data = <Transform>
      <DataSources>
        <Dataset id="TwitterPerson">
          <DatasetPlugin type="json">
            <Param name="file" value="twitter"/>
            <Param name="basePath" value=""/>
            <Param name="uriPattern" value="http://vocab.cs.uni-bonn.de/fuhsen/search/entity/twitter/{id}"/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="twitter">{cotent}
      </resource>
    </Transform>
    data
  }

}
