package controllers

import java.nio.ByteBuffer
import java.nio.file.Files
import javax.inject.Inject

import akka.util.Timeout
import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import forms.{ JobDefCreateForm, LoginForm, SignUpForm }
import models.C360DBService
import models.daos.NsJobDefinition
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.FormError
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.{ Action, AnyContent, Controller, MultipartFormData }
import play.libs.Time.CronExpression
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 */
class Application @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {
  private final val wrongSchedule = "Malformed Schedule string"
  private final val wrongArgs = "Malformed Args string"

  private implicit val timeout: Timeout = 5 seconds
  private implicit val nsJobDefinitionWrites = new Writes[NsJobDefinition] {
    def writes(jobDef: NsJobDefinition): JsObject = Json.obj(
      "job_definition_id" -> jobDef.job_definition_id,
      "job_name" -> jobDef.job_name,
      "job_type" -> jobDef.job_type,
      "job_priority" -> jobDef.job_priority,
      "cron_string" -> jobDef.cron_string
    )
  }

  def index: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity)))
  }

  def login: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.Application.index()))
      case None => Future.successful(Ok(views.html.login(LoginForm.loginForm)))
    }
  }

  def signUp: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.Application.index()))
      case None => Future.successful(Ok(views.html.signUp(SignUpForm.form)))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.Application.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  def createJobDefinition: Action[MultipartFormData[TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    val formRes: Either[Seq[FormError], NsJobDefinition] = JobDefCreateForm.form.bindFromRequest.fold(
      form => Left(form.errors),
      data =>
        Right(NsJobDefinition(
          "jd" + DateTime.now.getMillis / 1000,
          data.name,
          data.priority,
          DateTime.now,
          None,
          scheduled = true,
          0,
          data.`type`,
          data.cron_string,
          Some(DateTime.now))
        )
    )
    request.body.file("jarFile") match {
      case Some(f) =>
        Logger.warn("File uploaded: " + f.filename + ", " + f.contentType + ", " + f.ref.file.length() + ", " + f.ref.file.getPath)
        if (f.ref.file.exists() && f.ref.file.length() > 0 && f.contentType.exists(c => c.contains("java-archive"))) {
          formRes match {
            case Right(jobDef) =>
              if (!CronExpression.isValidExpression(jobDef.cron_string)) {
                Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", wrongSchedule)))
              } else {
                C360DBService.saveJobDefinition(jobDef.copy(jar = Some(ByteBuffer.wrap(Files.readAllBytes(f.ref.file.toPath))))) map {
                  _ =>
                    Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.created"))
                } recover {
                  case error =>
                    Logger.warn("Job Definition save failed: " + error.getMessage + "\n")
                    error.printStackTrace()
                    Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", error.getMessage))
                }
              }
            case Left(formErrors) =>
              Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", formErrors)))
          }
        } else {
          Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", "Jar file invalid")))
        }
      case None =>
        Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", "Jar file not uploaded!")))
    }
  }

  def modifyJobDefinition: Action[MultipartFormData[TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    val formRes: Either[Seq[FormError], NsJobDefinition] = JobDefCreateForm.form.bindFromRequest.fold(
      form => Left(form.errors),
      data =>
        Right(NsJobDefinition(
          data.jdid,
          data.name,
          data.priority,
          DateTime.now,
          None,
          scheduled = true,
          0,
          data.`type`,
          data.cron_string,
          Some(DateTime.now))
        )
    )
    request.body.file("jarFile") match {
      case Some(f) =>
        Logger.warn("File uploaded: " + f.filename + ", " + f.contentType + ", " + f.ref.file.length() + ", " + f.ref.file.getPath + ", " + f.ref.file.exists())
        formRes match {
          case Right(jobDef) =>
            if (!CronExpression.isValidExpression(jobDef.cron_string)) {
              Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", wrongSchedule)))
            } else {
              val mJobDef = if (f.ref.file.exists() && f.ref.file.length() > 0 && f.contentType.exists(c => c.contains("java-archive"))) {
                jobDef.copy(jar = Some(ByteBuffer.wrap(Files.readAllBytes(f.ref.file.toPath))))
              } else {
                jobDef
              }
              C360DBService.modifyJobDefinition(mJobDef) map {
                _ =>
                  Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.modified"))
              } recover {
                case error =>
                  Logger.warn("Job Definition save failed: " + error.getMessage + "\n")
                  error.printStackTrace()
                  Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", error.getMessage))
              }
            }
          case Left(formErrors) =>
            Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", formErrors)))
        }
      case None =>
        Future(Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", "Jar file not uploaded!")))
    }
  }

  def deleteJobDefinition(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    C360DBService.deleteJobDefinition(id) map {
      _ =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.deleted"))
    } recover {
      case error =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", error.getMessage))
    }
  }

  def enableJobDefinition(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    C360DBService.enableJobDefinition(id) map {
      _ =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.enabled"))
    } recover {
      case error =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", error.getMessage))
    }
  }

  def disableJobDefinition(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    C360DBService.disableJobDefinition(id) map {
      _ =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.disabled"))
    } recover {
      case error =>
        Redirect(routes.Application.listJobDefinitions()).flashing("result" -> Messages("jobDef.savefailed", error.getMessage))
    }
  }

  def getJobDefinition(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    C360DBService.getJobDefinition(id).map {
      result => Ok(Json.toJson(result))
    }
  }

  def listJobDefinitions: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    C360DBService.getAllJobDefinitions.map {
      result => Ok(views.html.job_definitions(request.identity, result, JobDefCreateForm.form))
    }
  }
}
