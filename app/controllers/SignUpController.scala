package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import forms.SignUpForm
import models.NsAdminService
import models.daos.NsAdmin
import org.joda.time.DateTime
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
 * c360sparkle
 * Created by ratnam on 5/14/16.
 */

/**
 * The sign up controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param adminService The admin service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUpController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  adminService: NsAdminService,
  authInfoRepository: AuthInfoRepository,
  passwordHasher: PasswordHasher,
  implicit val ec: ExecutionContext)
  extends Controller with I18nSupport {
  implicit val loginInfoFormat = Json.format[LoginInfo]

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def signUp = silhouette.UnsecuredAction.async { implicit request =>
    // XXX: Disabled signUp for prod as it needs to be done by the Dashboard
    // Future.successful(Redirect(routes.Application.login()).flashing("error" -> "SignUp Disabled!"))
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        adminService.retrieve(loginInfo).flatMap {
          case Some(admin) =>
            Future.successful(Redirect(routes.Application.signUp()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val admin = NsAdmin(
              id = (DateTime.now.getMillis / 1000L).asInstanceOf[Int] + "",
              login_info = Json.toJson(loginInfo).toString(),
              first_name = Some(data.firstName),
              last_name = Some(data.lastName),
              email = Some(data.email).get,
              password_info = "",
              url = Some(""), org = Some(""), role = Some(0), is_owner = true, login_count = 0, owner_id = Some(""),
              login_latest = Some(DateTime.now), login_current = Some(DateTime.now), login_ip = Some(""),
              created_at = Some(DateTime.now), updated_at = Some(DateTime.now), app_keys = Set(""), admins = Set("")
            )
            for {
              admin <- adminService.save(admin)
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield {
              silhouette.env.eventBus.publish(SignUpEvent(admin, request))
              silhouette.env.eventBus.publish(LoginEvent(admin, request))
              result
            }
        }
      }
    )
  }
}