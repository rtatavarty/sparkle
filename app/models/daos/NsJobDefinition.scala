package models.daos

import java.nio.ByteBuffer

import com.outworkers.phantom.dsl._

import scala.concurrent.Future

/**
 * c360sparkle
 * Created by ratnam on 7/14/17.
 */
case class NsJobDefinition(
  job_definition_id: String,
  job_name: String,
  job_priority: Int,
  created_at: DateTime,
  jar: Option[ByteBuffer],
  scheduled: Boolean,
  failed_count: Int,
  job_type: String,
  cron_string: String,
  start_at: Option[DateTime])

class NsJobDefinitions extends CassandraTable[INsJobDefinitions, NsJobDefinition] {
  object job_definition_id extends StringColumn(this) with PartitionKey
  object job_name extends StringColumn(this)
  object job_priority extends IntColumn(this)
  object created_at extends DateTimeColumn(this)
  object jar extends OptionalBlobColumn(this)
  object scheduled extends BooleanColumn(this)
  object failed_count extends IntColumn(this)
  object job_type extends StringColumn(this)
  object cron_string extends StringColumn(this)
  object start_at extends OptionalDateTimeColumn(this)
}

abstract class INsJobDefinitions extends NsJobDefinitions with RootConnector {
  override lazy val tableName = "job_definitions"

  def store(jd: NsJobDefinition): Future[ResultSet] = {
    insert()
      .value(_.job_definition_id, jd.job_definition_id)
      .value(_.job_name, jd.job_name)
      .value(_.job_priority, jd.job_priority)
      .value(_.created_at, jd.created_at)
      .value(_.jar, jd.jar)
      .value(_.scheduled, jd.scheduled)
      .value(_.failed_count, jd.failed_count)
      .value(_.job_type, jd.job_type)
      .value(_.cron_string, jd.cron_string)
      .value(_.start_at, jd.start_at)
      .future()
  }

  def updateJar(jobId: String, jar: ByteBuffer): Future[ResultSet] = {
    update
      .where(_.job_definition_id eqs jobId)
      .modify(_.jar setTo Some(jar))
      .future()
  }

  def getByID(jobId: String): Future[Option[NsJobDefinition]] = {
    select.where(_.job_definition_id eqs jobId).one()
  }

  def getAllJobDefinitions: Future[List[NsJobDefinition]] = {
    select.fetch()
  }

  def modifyJobDefinition(jd: NsJobDefinition): Future[ResultSet] = {
    if (jd.jar.exists(j => j.array().length > 0)) {
      update
        .where(_.job_definition_id eqs jd.job_definition_id)
        .modify(_.job_name setTo jd.job_name)
        .and(_.job_priority setTo jd.job_priority)
        .and(_.job_type setTo jd.job_type)
        .and(_.cron_string setTo jd.cron_string)
        .and(_.jar setTo jd.jar)
        .future()
    } else {
      update
        .where(_.job_definition_id eqs jd.job_definition_id)
        .modify(_.job_name setTo jd.job_name)
        .and(_.job_priority setTo jd.job_priority)
        .and(_.job_type setTo jd.job_type)
        .and(_.cron_string setTo jd.cron_string)
        .future()
    }
  }

  def enableJobDefinition(id: String): Future[ResultSet] = {
    update
      .where(_.job_definition_id eqs id)
      .modify(_.scheduled setTo true)
      .future()
  }

  def disableJobDefinition(id: String): Future[ResultSet] = {
    update
      .where(_.job_definition_id eqs id)
      .modify(_.scheduled setTo false)
      .future()
  }

  def deleteJobDefinition(jobId: String): Future[ResultSet] = {
    delete.where(_.job_definition_id eqs jobId).future()
  }
}
