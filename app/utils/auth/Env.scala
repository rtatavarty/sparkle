package utils.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.daos.NsAdmin

/**
 * c360sparkle
 * Created by ratnam on 5/24/16.
 */
trait DefaultEnv extends Env {
  type I = NsAdmin
  type A = CookieAuthenticator
}
