package controllers.de.fuhsen.engine

import java.io.{BufferedReader, FileReader}

import org.apache.jena.riot.Lang
import play.Logger
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import utils.dataintegration.RDFUtil

/**
  * Created by dcollarana on 8/3/2016.
  */
class SchemaController extends Controller {

  def getSourceList = Action {
    Logger.info("Source List")
    Ok(RDFUtil.modelToTripleString(JenaGlobalSchema.getDataSources, Lang.JSONLD))
  }

  def getEntityTypeList = Action {
    Logger.info("Entity Type List")
    Ok(RDFUtil.modelToTripleString(JenaGlobalSchema.getEntityTypes, Lang.JSONLD))
  }

}
