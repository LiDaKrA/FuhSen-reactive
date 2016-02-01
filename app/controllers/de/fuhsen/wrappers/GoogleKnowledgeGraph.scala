/*
 * Copyright (C) 2015 EIS Uni-Bonn
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

class GoogleKnowledgeGraph @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Google Knowledge Graph Search with query: " + query)

    val KEY = ConfigFactory.load.getString("kgb.app.key")

    val url = ConfigFactory.load.getString("kgb.url")
    val request: WSRequest = ws.url(url)
      .withQueryString("query" -> query)
      .withQueryString("key" -> KEY)
      .withQueryString("limit" -> "100")

    request.get().map { response =>
      Ok(response.body)
    }
  }

}
