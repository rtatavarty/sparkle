package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * c360sparkle
 * Created by ratnam on 12/23/15.
 */
object LoginForm {

  val loginForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "remember" -> boolean
    )(LoginData.apply)(LoginData.unapply)
  )

  case class LoginData(
    email: String,
    password: String,
    remember: Boolean)
}
