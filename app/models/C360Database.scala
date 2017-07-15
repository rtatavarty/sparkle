package models

/**
 * c360sparkle
 * Created by ratnam on 4/7/16.
 */

import java.nio.ByteBuffer

import com.mohiva.play.silhouette.api.LoginInfo
import com.outworkers.phantom.dsl._
import com.typesafe.config.ConfigFactory
import models.daos._
import play.api.Mode.Mode
import play.api.{ Logger, Mode }

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

object Connector {

  def getConnector(mode: Mode): KeySpaceDef = {
    Logger.info("Init Connector... [" + mode + "]")
    val config = ConfigFactory.load()
    val (hosts, keySpaceName, username, password) = mode match {
      case Mode.Dev =>
        (config.getStringList("cassandra.development.hosts"),
          config.getString("cassandra.development.keyspace"),
          if (config.hasPathOrNull("cassandra.development.username")) {
            if (config.getIsNull("cassandra.development.username")) {
              "" // handle null setting
            } else {
              config.getString("cassandra.development.username") // get and use non-null setting
            }
          } else {
            "" // handle entirely unset path
          },
          if (config.hasPathOrNull("cassandra.development.password")) {
            if (config.getIsNull("cassandra.development.password")) {
              ""
            } else {
              config.getString("cassandra.development.password")
            }
          } else {
            ""
          })
      case Mode.Test =>
        (config.getStringList("cassandra.test.hosts"),
          config.getString("cassandra.test.keyspace"),
          if (config.hasPathOrNull("cassandra.test.username")) {
            if (config.getIsNull("cassandra.test.username")) {
              ""
            } else {
              config.getString("cassandra.test.username")
            }
          } else {
            ""
          },
          if (config.hasPathOrNull("cassandra.test.password")) {
            if (config.getIsNull("cassandra.test.password")) {
              ""
            } else {
              config.getString("cassandra.test.password")
            }
          } else {
            ""
          })
      case Mode.Prod =>
        (config.getStringList("cassandra.production.hosts"),
          config.getString("cassandra.production.keyspace"),
          if (config.hasPathOrNull("cassandra.production.username")) {
            if (config.getIsNull("cassandra.production.username")) {
              ""
            } else {
              config.getString("cassandra.production.username")
            }
          } else {
            ""
          },
          if (config.hasPathOrNull("cassandra.production.password")) {
            if (config.getIsNull("cassandra.production.password")) {
              ""
            } else {
              config.getString("cassandra.production.password")
            }
          } else {
            ""
          })
    }

    Logger.info("Connector Info: " + keySpaceName + "@" + hosts)
    if (username.isEmpty) {
      ContactPoints.apply(hosts.asInstanceOf[java.util.List[String]])
        .keySpace(keySpaceName.asInstanceOf[String])
    } else {
      ContactPoints.apply(hosts.asInstanceOf[java.util.List[String]])
        .withClusterBuilder(_.withCredentials(username, password))
        .keySpace(keySpaceName.asInstanceOf[String])
    }
  }
  var mode: Option[Mode] = None
  lazy val connector: KeySpaceDef = getConnector(mode.getOrElse(Mode.Dev))
}

/**
 * C360Database class holding objects of each table models (hence prefixed 'm')
 *
 * @param keyspace
 *                 Provides the Connector definition
 */
class C360Database(val keyspace: KeySpaceDef) extends Database[C360Database](keyspace) {

  object mNsAdmins extends INsAdmins with keyspace.Connector
  object mNsJobDefinitions extends INsJobDefinitions with keyspace.Connector
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
    database.create(15.seconds)
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

  def saveJobDefinition(jobDefinition: NsJobDefinition): Future[ResultSet] = {
    database.mNsJobDefinitions.store(jobDefinition)
  }

  def updateJar(jobId: String, jar: ByteBuffer): Future[ResultSet] = {
    database.mNsJobDefinitions.updateJar(jobId, jar)
  }

  def getJobDefinition(jobId: String): Future[Option[NsJobDefinition]] = {
    database.mNsJobDefinitions.getByID(jobId)
  }

  def getAllJobDefinitions: Future[List[NsJobDefinition]] = {
    database.mNsJobDefinitions.getAllJobDefinitions
  }

  def deleteJobDefinition(jobId: String): Future[ResultSet] = {
    database.mNsJobDefinitions.deleteJobDefinition(jobId)
  }
}

object C360DBService extends C360DBService with C360DatabaseProvider
