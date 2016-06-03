package controllers.de.fuhsen.common

sealed trait ApiResponse

case class ApiError(statusCode: Int, errorMessage: String) extends ApiResponse

case class ApiSuccess(responseBody: String) extends ApiResponse