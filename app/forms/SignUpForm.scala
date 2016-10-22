package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * c360sparkle
 * Created by ratnam on 5/14/16.
 */
object SignUpForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(6)
    )(SignUpData.apply)(SignUpData.unapply)
  )

  case class SignUpData(
    firstName: String,
    lastName: String,
    email: String,
    password: String)
}
