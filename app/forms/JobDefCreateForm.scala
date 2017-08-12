package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * c360seeder
 * Created by ratnam on 11/4/16.
 */
object JobDefCreateForm {

  val form = Form(
    mapping(
      "jdid" -> nonEmptyText,
      "name" -> nonEmptyText,
      "priority" -> number(min = 0, max = 10),
      "type" -> nonEmptyText,
      "cron_string" -> nonEmptyText
    )(JobDefData.apply)(JobDefData.unapply)
  )

  case class JobDefData(
    jdid: String,
    name: String,
    priority: Int,
    `type`: String,
    cron_string: String)
}
