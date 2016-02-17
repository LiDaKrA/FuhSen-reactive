package controllers.de.fuhsen.wrappers

import play.api.libs.oauth.{RequestToken, ConsumerKey}

/**
 * Defines OAuth related methods.
 */
trait RestApiOAuthTrait {
  def oAuthConsumerKey: ConsumerKey

  def oAuthRequestToken: RequestToken
}
