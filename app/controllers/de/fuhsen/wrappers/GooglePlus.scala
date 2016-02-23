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

import java.io.{StringWriter, ByteArrayInputStream}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import play.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import scala.concurrent.Future

import scala.xml.Elem

case class Person(id: String, displayName: String)

class GooglePlus @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Google Plus Search with query: " + query)

    //Preparing Google+ API call
    val KEY = ConfigFactory.load.getString("gplus.app.key")
    val URL = ConfigFactory.load.getString("gplus.user.url")
    val apiRequest: WSRequest = ws.url(URL)
                                  .withQueryString("query" -> query)
                                  .withQueryString("key" -> KEY)

    //Preparing Transformation task call
    val TRANSFORMTASKURL = ConfigFactory.load.getString("silk.server.url") + ConfigFactory.load.getString("gplus.person.transform.url")
    val TRANSFORMORGTASKURL = ConfigFactory.load.getString("silk.server.url") + ConfigFactory.load.getString("gplus.organization.transform.url")

    /*
    val transformRequest: WSRequest = ws.url(TRANSFORMTASKURL)
                                        .withHeaders("Content-Type" -> "application/xml")
                                        .withHeaders("Accept" -> "application/ld+json")
    */

    implicit val peopleReader = Json.reads[Person]

    for {
      responseApi <- apiRequest.get()
      people = (responseApi.json \ "items").as[List[Person]]
      result <- Future.sequence( people.map { person =>
        for {
          //Google plus get person request
          responseUserApi <- ws.url(URL+"/"+person.id)
                              .withQueryString("key" -> KEY)
                              .get()

          //Transform user information
          data = dataTranformationBody(responseUserApi.body)
          responseUserTransformation <- ws.url(TRANSFORMTASKURL)
                                      .withHeaders("Content-Type" -> "application/xml")
                                      .withHeaders("Accept" -> "application/ld+json")
                                      .post(data)

          //Transform organizations information
          data = organizationsTranformationBody(responseUserApi.body)
          responseOrgTransformation <- ws.url(TRANSFORMORGTASKURL)
                                          .withHeaders("Content-Type" -> "application/xml")
                                          .withHeaders("Accept" -> "application/ld+json")
                                          .post(data)

        }yield {
          Logger.info("Id: "+person.id)
          generateModelResult(responseUserTransformation.body, responseOrgTransformation.body, null)
        }
      })
    }yield {
      val resultModel = ModelFactory.createDefaultModel()
      result.foreach( r => resultModel.add(r))
      val output: StringWriter = new StringWriter()
      resultModel.write(output, "JSON-LD")
      Ok(output.toString())

    }

    /*apiRequest.get().map { response =>
      Ok(response.body)
    }*/

  }

  private def generateModelResult (userContent: String, organizationContent: String, placesContent : String) : Model = {
    val resultModel = ModelFactory.createDefaultModel()

    if (userContent != null && !userContent.isEmpty())
      //Logger.info(userContent)
      resultModel.add( ModelFactory.createDefaultModel().read(new ByteArrayInputStream(userContent.getBytes()), null, "JSON-LD"))
    if (organizationContent != null && !organizationContent.isEmpty()) {
      //Logger.info(organizationContent)
      resultModel.add(ModelFactory.createDefaultModel().read(new ByteArrayInputStream(organizationContent.getBytes()), null, "JSON-LD"))
    }
    if (placesContent != null && !placesContent.isEmpty())
      resultModel.add( ModelFactory.createDefaultModel().read(new ByteArrayInputStream(placesContent.getBytes()), null, "JSON-LD"))

    resultModel
  }

  private def dataTranformationBody (content: String) : Elem = {
    Logger.info(content)
    val data = <Transform>
      <DataSources>
        <Dataset id="GPlusGetPerson">
          <DatasetPlugin type="json">
            <Param name="file" value="gplusPerson"/>
            <Param name="basePath" value=""/>
            <Param name="uriPattern" value="http://vocab.cs.uni-bonn.de/fuhsen/search/entity/gplus/{id}"/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="gplusPerson">{content}
      </resource>
    </Transform>
    data
  }

  private def organizationsTranformationBody (content: String) : Elem = {
    val data = <Transform>
      <DataSources>
        <Dataset id="GPlusOrganizationsOfPerson">
          <DatasetPlugin type="json">
            <Param name="file" value="gplusOrganization"/>
            <Param name="basePath" value="organizations"/>
            <Param name="uriPattern" value=""/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="gplusOrganization">{content}
      </resource>
    </Transform>
    data
  }

}
