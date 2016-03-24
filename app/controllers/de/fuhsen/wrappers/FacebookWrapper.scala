package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.wrappers.dataintegration.{SilkTransformableTrait, SilkTransformationTask}

/**
  * Created by cmorales on 16.03.2016.
  */
class FacebookWrapper extends RestApiWrapperTrait with RestApiOAuth2Trait with SilkTransformableTrait{

  //RestApiWrapperTrait implementation:
  /** Query parameters that should be added to the request. */
  override def queryParams: Map[String, String] = Map("type" -> "user",
                                                      "fields" -> ConfigFactory.load.getString("facebook.search.fields"))
  /** Returns for a given query string the representation as query parameter for the specific API. */
  override def searchQueryAsParam(queryString: String): Map[String, String] = {
    val query_string: String = queryString.replace(" ", "%20")
    Map("q" -> query_string)
  }
  /** The REST endpoint URL */
  override def apiUrl: String = ConfigFactory.load.getString("facebook.search.url")
  override def sourceLocalName: String = "facebook"

  //RestApiOAuth2Trait implementation:
  override def oAuth2ClientKey : String =  ConfigFactory.load.getString("facebook.app.key")
  override def oAuth2ClientSecret : String =  ConfigFactory.load.getString("facebook.app.secret")
  override def oAuth2AccessToken : String =  TokenManager.getAccesTokenByProvider("facebook")

  //SilkTransformableTrait implementation:
  override def silkTransformationRequestTasks = Seq(
    SilkTransformationTask(
      transformationTaskId = "FacebookPersonTransformation",
      createSilkTransformationRequestBody(
        basePath = "",
        uriPattern = "http://vocab.cs.uni-bonn.de/fuhsen/search/entity/facebook/{id}"
      )
    )
  )
  /** The type of the transformation input. */
  override def datasetPluginType: DatasetPluginType = DatasetPluginType.JsonDatasetPlugin
  /** The project id of the Silk project */
  override def projectId: String = ConfigFactory.load.getString("silk.socialApiProject.id")
}