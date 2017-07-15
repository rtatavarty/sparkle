package models.daos

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import com.outworkers.phantom.dsl._
import play.api.libs.json.Json

import scala.concurrent.Future

/**
 * c360sparkle
 * Created by ratnam on 5/11/16.
 */
case class NsAdmin(
  id: String,
  login_info: String,
  password_info: String,
  first_name: Option[String],
  last_name: Option[String],
  email: String,
  url: Option[String],
  org: Option[String],
  role: Option[Int],
  is_owner: Boolean,
  owner_id: Option[String],
  login_count: Int,
  login_latest: Option[DateTime],
  login_current: Option[DateTime],
  login_ip: Option[String],
  created_at: Option[DateTime],
  updated_at: Option[DateTime],
  app_keys: Set[String],
  admins: Set[String]) extends Identity

class NsAdmins extends CassandraTable[INsAdmins, NsAdmin] {
  object id extends StringColumn(this) with PartitionKey
  object login_info extends StringColumn(this)
  object password_info extends StringColumn(this)
  object first_name extends OptionalStringColumn(this)
  object last_name extends OptionalStringColumn(this)
  object email extends StringColumn(this)
  object url extends OptionalStringColumn(this)
  object org extends OptionalStringColumn(this)
  object role extends OptionalIntColumn(this)
  object is_owner extends BooleanColumn(this)
  object owner_id extends OptionalStringColumn(this)
  object login_count extends IntColumn(this)
  object login_latest extends OptionalDateTimeColumn(this)
  object login_current extends OptionalDateTimeColumn(this)
  object login_ip extends OptionalStringColumn(this)
  object created_at extends OptionalDateTimeColumn(this)
  object updated_at extends OptionalDateTimeColumn(this)
  object app_keys extends SetColumn[String](this)
  object admins extends SetColumn[String](this)
}

abstract class INsAdmins extends NsAdmins with RootConnector {
  override lazy val tableName = "ns_admins"

  def createTable(): Future[ResultSet] = {
    create.ifNotExists().future()
  }

  def store(nsAdmin: NsAdmin): Future[ResultSet] = {
    insert
      .value(_.id, nsAdmin.id)
      .value(_.login_info, nsAdmin.login_info)
      .value(_.password_info, nsAdmin.password_info)
      .value(_.first_name, nsAdmin.first_name)
      .value(_.last_name, nsAdmin.last_name)
      .value(_.email, nsAdmin.email)
      .value(_.url, nsAdmin.url)
      .value(_.org, nsAdmin.org)
      .value(_.role, nsAdmin.role)
      .value(_.is_owner, nsAdmin.is_owner)
      .value(_.owner_id, nsAdmin.owner_id)
      .value(_.login_count, nsAdmin.login_count)
      .value(_.login_latest, nsAdmin.login_latest)
      .value(_.login_current, nsAdmin.login_current)
      .value(_.login_ip, nsAdmin.login_ip)
      .value(_.created_at, nsAdmin.created_at)
      .value(_.updated_at, nsAdmin.updated_at)
      .value(_.app_keys, nsAdmin.app_keys)
      .value(_.admins, nsAdmin.admins)
      .future()
  }

  def getById(id: String): Future[Option[NsAdmin]] = {
    select.where(_.id eqs id).one()
  }

  def getByLoginInfo(loginInfo: LoginInfo): Future[Option[NsAdmin]] = {
    val liStr = Json.toJson(loginInfo).toString()
    val res = select.fetch()
    /* Fetch all admins and pick the one with matching loginInfo json. */
    res.map { admins: Seq[NsAdmin] =>
      admins.find(admin => admin.login_info.equals(liStr))
    }
  }
}
