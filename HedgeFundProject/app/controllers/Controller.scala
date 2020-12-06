package controllers

import actors._
import akka.actor.{ActorSystem, Props}
import akka.actor.ActorSystem
import akka.util.Timeout
import javax.inject._
import models.login.LoginHandler
import models.DAO.UserTable
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api.data.format.Formats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

// Case class to validate login form
case class LoginForm(username: String, password: String)

// Case class to validate sign-up form
case class SignUpForm(username: String, password: String, name: String, email: String)

@Singleton
class HomeController @Inject()(system: ActorSystem,cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  //actors
  val bossActor = ActorSystem().actorOf(Props[BossActor])

  implicit val timeout: Timeout = 5 minutes

  val loginData = Form(mapping(
    "Username" -> nonEmptyText,
    "Password" -> nonEmptyText
  )(LoginForm.apply)(LoginForm.unapply))

  val signupData = Form(mapping(
    "Username" -> nonEmptyText,
    "Password" -> nonEmptyText,
    "Name" -> nonEmptyText,
    "Email" -> email
  )(SignUpForm.apply)(SignUpForm.unapply))

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def login(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.credentials.login(loginData))
  }

  def validateLogin(): Action[AnyContent] = Action.async { implicit request =>
    loginData.bindFromRequest.fold(
      formWithError => Future(BadRequest(views.html.credentials.login(formWithError))),
      ld => {
         val handler = new LoginHandler() with UserTable
         (handler.validateUser(ld.username, ld.password)).map(b => b match {
           case true => Redirect(routes.HomeController.action())
           case false => Redirect(routes.HomeController.login()).flashing("error" -> s"**Username or password is incorrect")
      })
      })
  }

  def signup(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.credentials.signup(signupData))
  }

  def validateSignUp(): Action[AnyContent] = Action.async { implicit request =>
    signupData.bindFromRequest.fold(
      formWithError => Future(BadRequest(views.html.credentials.signup(formWithError))),
      sgd => {
        val handler = new LoginHandler() with UserTable
        (handler.addUser(sgd.username, sgd.password, sgd.name, sgd.email,sgd.username+"1",100000.0)).map(b => b match {
          case true => Redirect(routes.HomeController.login())
          case false => Redirect(routes.HomeController.signup()).flashing("error" -> s"**Username already exists")
      })
  })
  }

  def action(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.Action.action())
  }
}
