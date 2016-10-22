package modules

import com.google.inject.AbstractModule
import models.{ C360DBService, Connector }
import play.api.{ Configuration, Environment, Logger }

/**
 * c360sparkle
 * Created by ratnam on 4/10/16.
 */

trait C360Init {}

class C360InitClass extends C360Init {
  initializeDB()

  def initializeDB() = {
    Logger.info("C360INIT: Creating database...")
    C360DBService.createDatabase()
    Logger.info("C360INIT: Creating tables...")
    C360DBService.createTables()
  }
}

class C360Module(environment: Environment,
  configuration: Configuration) extends AbstractModule {
  override def configure() = {
    // Select the right DB Connector based on Mode.
    Connector.mode = Some(environment.mode)
    bind(classOf[C360Init]).to(classOf[C360InitClass]).asEagerSingleton()
  }
}
