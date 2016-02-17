package controllers.de.fuhsen.wrappers

/**
 * The different input types for the Silk component. This is usually the content type that comes back from
 * the wrapped REST APIs.
 */
sealed case class DatasetPluginType(id: String)

object DatasetPluginType {

  object JsonDatasetPlugin extends DatasetPluginType("json")

  object XmlDatasetPlugin extends DatasetPluginType("xml")

}