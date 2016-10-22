package models

/**
 * c360sparkle
 * Created by ratnam on 4/7/16.
 */

import com.mohiva.play.silhouette.api.LoginInfo
import com.typesafe.config.ConfigFactory
import com.websudos.phantom.dsl._
import models.daos._
import play.api.Mode.Mode
import play.api.{ Logger, Mode }

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

object Connector {

  def getConnector(mode: Mode) = {
    Logger.info("Init Connector... [" + mode + "]")
    val config = ConfigFactory.load()
    val (hosts, keySpaceName, username, password) = mode match {
      case Mode.Dev =>
        (config.getStringList("cassandra.development.hosts"),
          config.getString("cassandra.development.keyspace"),
          config.getString("cassandra.development.username"),
          config.getString("cassandra.development.password"))
      case Mode.Test =>
        (config.getStringList("cassandra.test.hosts"),
          config.getString("cassandra.test.keyspace"),
          config.getString("cassandra.test.username"),
          config.getString("cassandra.test.password"))
      case Mode.Prod =>
        (config.getStringList("cassandra.production.hosts"),
          config.getString("cassandra.production.keyspace"),
          config.getString("cassandra.production.username"),
          config.getString("cassandra.production.password"))
    }

    ContactPoints.apply(hosts.asInstanceOf[java.util.List[String]])
      .withClusterBuilder(_.withCredentials(username, password))
      .keySpace(keySpaceName.asInstanceOf[String])
  }
  var mode: Option[Mode] = None
  lazy val connector = getConnector(mode.getOrElse(Mode.Dev))
}

/**
 * C360Database class holding objects of each table models (hence prefixed 'm')
 *
 * @param keyspace
 *                 Provides the Connector definition
 */
class C360Database(val keyspace: KeySpaceDef) extends Database(keyspace) {

  object mNsAdmins extends INsAdmins with keyspace.Connector
}

object C360Database extends C360Database(Connector.connector)

trait C360DatabaseProvider {
  def database: C360Database = C360Database
}

trait C360DBService extends C360DatabaseProvider with Connector.connector.Connector {

  /**
   * Create the database if not exists
   * Called during application startup
   *
   * @return
   */
  def createDatabase(): Unit = {
    Await.result(database.autocreate.future(), 5.seconds)
  }

  private def createTable(table: CassandraTable[_, _]): Unit = {
    val f = table.create.ifNotExists().future()
    val r = Await.ready(f, 5 seconds).value.get
    r match {
      case Success(result) =>
        Logger.info("Table create: " + table.tableName + ": OK")
      case Failure(failure) =>
        Logger.error("Table create: " + table.tableName + ": ERROR\n" + failure.getMessage)
    }
  }

  /**
   * Create the tables if not exists
   * Called during application startup
   *
   * @return
   */
  def createTables(): Unit = {

    for (table <- database.tables) {
      createTable(table)
    }
  }

  def saveAdmin(nsAdmin: NsAdmin): Future[NsAdmin] = {
    val res = database.mNsAdmins.store(nsAdmin)
    /*
    * Since admin info and password info are in the same table,
    * wait until the admin info is saved before looking up password info
    */
    Await.ready(res, 10 seconds)
    Future { nsAdmin }
  }

  def findAdmin(loginInfo: LoginInfo): Future[Option[NsAdmin]] = {
    database.mNsAdmins.getByLoginInfo(loginInfo)
  }

}

object C360DBService extends C360DBService with C360DatabaseProvider
