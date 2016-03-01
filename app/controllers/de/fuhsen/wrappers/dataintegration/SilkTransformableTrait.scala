package controllers.de.fuhsen.wrappers.dataintegration

import com.typesafe.config.ConfigFactory
import controllers.de.fuhsen.wrappers.DatasetPluginType

import scala.xml.Elem

/**
  * Specifies how to transform content via a Silk transformation
  */
trait SilkTransformableTrait {
  /**
    * One or more transformation tasks that should be executed.
    *
    * @return
    */
  def silkTransformationRequestTasks: Seq[SilkTransformationTask]

  def transformationEndpoint(transformationTaskId: String) = {
    s"""$silkServerUrl/transform/tasks/$projectId/$transformationTaskId/transformInput"""
  }

  private def silkServerUrl = {
    ConfigFactory.load.getString("silk.server.url")
  }

  /**
    * The project id of the Silk project
    */
  def projectId: String

  /**
    * The type of the transformation input.
    */
  def datasetPluginType: DatasetPluginType

  // Helper method to create the transformation request body function via currying
  protected def createSilkTransformationRequestBody(basePath: String,
                                                    uriPattern: String)
                                                   (content: String): Elem = {
    <Transform>
      <DataSources>
        <Dataset id="InputData">
          <DatasetPlugin type={datasetPluginType.id}>
            <Param name="file" value="inputFile"/>
            <Param name="basePath" value={basePath}/>
            <Param name="uriPattern" value={uriPattern}/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="inputFile">
        {content}
      </resource>
    </Transform>
  }
}

//
/**
  * Holds the data for a Silk transformation request.
  *
  * @param transformationTaskId                   The task id of the transformation task of the project.
  * @param silkTransformationRequestBodyGenerator a function that generates the XML request body having the API response
  *                                               as parameter. Use the curried version of [[SilkTransformableTrait.createSilkTransformationRequestBody]]
  *                                               to create these functions.
  */
case class SilkTransformationTask(transformationTaskId: String,
                                  silkTransformationRequestBodyGenerator: String => Elem)