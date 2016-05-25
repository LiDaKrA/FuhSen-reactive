package controllers.de.fuhsen.engine

import play.Logger
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.json._

/**
  * Created by dcollarana on 5/23/2016.
  */
class QueryProcessingTask extends MicroTaskTrait {
  /**
    * Returns the execution order.
    */
  override def order: Int = 1

  /**
    * The REST endpoint URL
    */
  override def microTaskUrl: String = "/api/microservices/queryprocessing"


}
