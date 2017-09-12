package controllers.de.fuhsen.engine

/**
  * Created by dcollarana on 6/5/2016.
  */

import org.apache.jena.rdf.model.Model
import scala.collection.mutable.HashMap
import play.Logger

trait StorageTrait {

  def saveModel(uid :String, model : Model)

  def updateModel(uid :String, model : Model)

  def deleteModel(uid :String)

  def getModel(uid :String) : Option[Model]

  def size() : Integer

}

object GraphResultsCache extends StorageTrait {

  private var searchList = new HashMap[String, Model]

  override def saveModel(uid: String, model: Model) {
    searchList.+=((uid, model))
  }

  override def deleteModel(uid: String): Unit = {
    searchList.get(uid) match {
      case Some(model) =>
        model.close()
        searchList.-=(uid)
      case None =>
        Logger.warn("No model found, nothing was deleted")
    }
  }

  override def getModel(uid: String): Option[Model] = searchList.get(uid)

  override def updateModel(uid: String, model: Model) {
    deleteModel(uid)
    saveModel(uid, model)
  }

  override def size(): Integer = {
    searchList.size
  }

}
