package controllers.de.fuhsen.wrappers

/**
  * A trait for specifying that an API is paginating, i.e. it is possible to request the complete result in pages.
  */
trait PaginatingApiTrait { this: RestApiWrapperTrait =>

  /**
    * Extracts and returns the next page/offset value from the response body of the API.
    * @param resultBody The body serialized as String as coming from the API.
    * @param apiUrl The last value. This can be used if the value is not available in the result body, but instead
    *                  is calculated by the wrapper implementation.
    */
  def extractNextPageQueryValue(resultBody: String, apiUrl: Option[String]): Option[String]
}
