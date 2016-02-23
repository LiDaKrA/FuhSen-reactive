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

import javax.inject.Inject

import play.Logger
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.json._

class SearchEngine @Inject() (ws: WSClient) extends Controller {

  def search(query: String) = Action {

    Logger.info("Starting Search Engine Search with query: " + query)
    Ok("Hello " + query)

  }

  def expandQuery(query: String) = Action {

    Logger.info("Expanding Search Query: " + query)

    val data = Json.obj(
      "key1" -> query,
      "key2" -> "value2"
    )

    Ok(data)

  }

}
