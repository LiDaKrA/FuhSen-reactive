package controllers.de.fuhsen.wrappers.dataintegration

import com.typesafe.config.ConfigFactory
import org.apache.jena.rdf.model.Model
import play.api.libs.ws.WSClient

/**
  * Created on 2/29/16.
  */
class EntityLinking(silkConfig: SilkConfig) {
//  def linkEntities(entityRDF: Model)
//                  (implicit wsClient: WSClient): Traversable[Triple] = {
//    wsClient.
//  }

  def linkTemplate(triples: String, tripleFormat: String = "Turtle") = {
    <Link>
      <!-- curl -i -H 'content-type: application/xml' -X POST http://localhost:9000/linking/tasks/SocialAPIMappings/linkPerson/postLinkDatasource -d @linkPersonRequest.xml -->
      <DataSources>
        <Dataset id="sourceDataset">
          <DatasetPlugin type="file">
            <Param name="file" value="source"/>
            <Param name="format" value= {tripleFormat} />
          </DatasetPlugin>
        </Dataset>
        <Dataset id="targetDataset">
          <DatasetPlugin type="file">
            <Param name="file" value="target"/>
            <Param name="format" value={tripleFormat}/>
          </DatasetPlugin>
        </Dataset>
      </DataSources>
      <resource name="source">
        {triples}
      </resource>
      <resource name="target">
        {triples}
      </resource>
    </Link>
  }
}


case class SilkConfig(projectId: String,
                      linkingTaskId: String,
                      silkServerUrl: String = ConfigFactory.load.getString("silk.server.url")) {

  def endpoint = s"""$silkServerUrl/transform/tasks/$projectId/$linkingTaskId/transformInput"""
}