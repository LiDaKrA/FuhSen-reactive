package controllers.de.fuhsen.wrappers

/**
 * Created by andreas on 2/15/16.
 */
trait RestApiWrapperTrait {
  /**
   * Query parameters that should be added to the request.
   * @return
   */
  def queryParams: Map[String, String]

  /**
   * The REST endpoint URL
   * @return
   */
  def apiUrl: String

  /**
   * Returns for a given query string the representation as query parameter for the specific API.
   * 
   * @param queryString
   * @return
   */
  def searchQueryAsParam(queryString: String): Map[String, String]
}