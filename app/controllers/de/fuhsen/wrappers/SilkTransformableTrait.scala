package controllers.de.fuhsen.wrappers

import com.typesafe.config.ConfigFactory

import scala.xml.Elem

/**
 * Specifies how to transform content via a Silk transformation
 */
trait SilkTransformableTrait {
  def silkTransformationRequestBody(content: String): Elem

  protected def createSilkTransformationRequestBody(content: String,
                                                    basePath: String,
                                                    uriPattern: String): Elem = {
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

  def transformationEndpoint = s"""$silkServerUrl/transform/tasks/$projectId/$transformationTaskId/transformInput"""

  private def silkServerUrl = ConfigFactory.load.getString("silk.server.url")

  /**
   * The project id of the Silk project
   */
  def projectId: String

  /**
   * The task id of the transformation task of the project.
   */
  def transformationTaskId: String

  /**
   * The type of the transformation input.
   */
  def datasetPluginType: DatasetPluginType
}