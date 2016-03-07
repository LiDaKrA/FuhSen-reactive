/*
 * Copyright (C) 2016 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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