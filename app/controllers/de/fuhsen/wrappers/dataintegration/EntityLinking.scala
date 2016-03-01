package controllers.de.fuhsen.wrappers.dataintegration

import com.typesafe.config.ConfigFactory

/**
  * Created on 2/29/16.
  */
class EntityLinking(silkConfig: SilkConfig) {
  def linkTemplate(triples: String, tripleFormat: String = "Turtle") = {
    <Link>
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

  def endpoint = s"""$silkServerUrl/linking/tasks/$projectId/$linkingTaskId/postLinkDatasource"""
}