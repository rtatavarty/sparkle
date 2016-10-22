package models

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.daos.NsAdmin
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.concurrent.Future

/**
 * c360sparkle
 * Created by ratnam on 12/22/15.
 */
trait NsAdminService extends IdentityService[NsAdmin] {
  def save(nsAdmin: NsAdmin): Future[NsAdmin]
}

class NsAdminServiceImpl extends NsAdminService {
  def save(nsAdmin: NsAdmin): Future[NsAdmin] = C360DBService.saveAdmin(nsAdmin)
  def retrieve(loginInfo: LoginInfo): Future[Option[NsAdmin]] = C360DBService.findAdmin(loginInfo)
}

class NsAdminPwdDAO extends DelegableAuthInfoDAO[PasswordInfo] {
  implicit val passwordInfoFormat = Json.format[PasswordInfo]

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    C360DBService.findAdmin(loginInfo).flatMap {
      case None =>
        Future.successful(Option.empty[PasswordInfo])
      case Some(nsAdmin) =>
        Future(Some(Json.parse(nsAdmin.password_info).as[PasswordInfo]))
    }
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    C360DBService.findAdmin(loginInfo).flatMap {
      case None =>
        Logger.info("Save Admin with email " + loginInfo.providerKey + " not found.")
        Future.successful(authInfo) // Dummy return
      case Some(nsAdmin) =>
        Logger.info("Save Admin with email " + loginInfo.providerKey + " found.")
        val passwordInfo = Json.toJson(authInfo).toString()
        val newAdmin = nsAdmin.copy(password_info = passwordInfo)
        C360DBService.saveAdmin(newAdmin) // TODO: check return value
        Future.successful(authInfo)
    }
  }

  // TODO: implement remove
  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(())
}
