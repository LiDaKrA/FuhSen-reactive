import java.io.FileReader

import controllers.de.fuhsen.engine.JenaGlobalSchema
import play.api.Play._
import play.api.{GlobalSettings, Application}
import play.Logger

/**
  * Created by dcollarana on 8/3/2016.
  */
object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application is started!!!")
    val globalSchema = current.getFile("schema/ontofuhsen.ttl")
    val fileReader = new FileReader(globalSchema)
    JenaGlobalSchema.load(fileReader)
    Logger.info("Model loaded: "+JenaGlobalSchema.isLoaded())
  }

}
