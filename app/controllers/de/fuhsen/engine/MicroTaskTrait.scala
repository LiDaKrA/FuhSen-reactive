package controllers.de.fuhsen.engine

/**
  * Created by dcollarana on 5/23/2016.
  */

trait MicroTaskTrait {

  /**
    * Returns the execution order.
    */
  def order: Int

  /**
    * The REST endpoint URL
    */
  def microTaskUrl: String

}
