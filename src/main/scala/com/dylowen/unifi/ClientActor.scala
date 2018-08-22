package com.dylowen.unifi

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.dylowen.nanoleaf.NanoSystem
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object ClientActor {

  sealed trait Message

  private case class RefreshClientAuth(backoff: FiniteDuration = 1 second) extends Message

  private case class SetAuth(auth: UnifiAuthorization) extends Message

  case class UnifiRequest[T](request: UnifiAuthorization => Future[T], ref: ActorRef[Try[T]]) extends Message


  private val TimerKey: Unit = ()
}

class ClientActor(implicit nanoSystem: NanoSystem) extends LazyLogging {

  import ClientActor._

  val behavior: Behavior[Message] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Message]) => {
      //timers.startPeriodicTimer(RefreshInterval, RefreshNetworkInterfaces, RefreshInterval)

      Behaviors.setup((prestart: ActorContext[Message]) => {
        import prestart.executionContext

        // kick off a login
        login(prestart, 1 millisecond, timers)

        // mark ourselves as logging in
        gettingAuth(timers, Seq())
      })
    })
  ).onFailure(SupervisorStrategy.restart)

  private def hasAuth(auth: UnifiAuthorization,
                      timers: TimerScheduler[Message]): Behavior[Message] = Behaviors.receive[Message](
    (ctx: ActorContext[Message], message: Message) => {
      import ctx.executionContext

      message match {
        case SetAuth(nextAuth) => {
          setAuth(nextAuth, timers)
        }
        case request: UnifiRequest[_] => {
          logger.debug("Received request")

          processRequest(request, auth)

          Behaviors.same[Message]
        }
        case RefreshClientAuth(backoff) => {
          logger.debug("Refresh client auth")

          login(ctx, backoff, timers)

          gettingAuth(timers, Seq())
        }
      }
    }
  )

  private def gettingAuth(timers: TimerScheduler[Message],
                          pendingRequests: Seq[UnifiRequest[_]]): Behavior[Message] = Behaviors.receive[Message](
    (ctx: ActorContext[Message], message: Message) => {
      import ctx.executionContext

      message match {
        case RefreshClientAuth(backoff) => {
          logger.debug("Refresh client auth")

          login(ctx, backoff, timers)

          Behaviors.same
        }
        case SetAuth(auth) => {
          // respond to all our pending requests
          processPendingRequests(pendingRequests, auth)

          setAuth(auth, timers)
        }
        case request: UnifiRequest[_] => {
          logger.debug("Received request")

          // we don't have any auth so add our request to the pending requests
          gettingAuth(timers, pendingRequests :+ request)
        }
      }
    }
  )

  private def setAuth(auth: UnifiAuthorization,
                      timers: TimerScheduler[Message]): Behavior[Message] = {
    // schedule our next auth refresh
    val nextRefresh: FiniteDuration = auth.expiration

    logger.debug(s"Setting new auth for ${auth.username}, scheduling refresh in $nextRefresh")

    timers.startSingleTimer(TimerKey, RefreshClientAuth(), nextRefresh)

    hasAuth(auth, timers)
  }

  private def login(ctx: ActorContext[Message],
                    backoff: FiniteDuration,
                    timers: TimerScheduler[Message])
                   (implicit ec: ExecutionContext): Unit = {
    UnifiAuthorization()
      .onComplete({
        case Success(result) => result match {
          case Right(nextAuth) => {
            ctx.self ! SetAuth(nextAuth)
          }
          case Left(authError) => {
            retryLogin(backoff,
              (nextTry: FiniteDuration) => {
                logger.error(s"Failed to login to unifi. Trying again in ${nextTry.toCoarsest} error: ${authError.message}")
                logger.trace(authError.response.toString)
              },
              timers)
          }
        }
        case Failure(exception) => {
          retryLogin(backoff,
            (nextTry: FiniteDuration) => {
              logger.error(s"Failed to login to unifi. Trying again in ${nextTry.toCoarsest}", exception)
            },
            timers)
        }
      })
  }

  private def retryLogin(backoff: FiniteDuration, logFn: FiniteDuration => Unit, timers: TimerScheduler[Message]): Unit = {
    val nextTry: FiniteDuration = backoff * 2

    logFn(nextTry)

    timers.startSingleTimer(TimerKey, RefreshClientAuth(nextTry), nextTry)
  }

  /*
  def withState(maybeAuth: Option[UnifiAuthorization],
                timers: TimerScheduler[Message],
                pendingRequests: Seq[UnifiRequest[_]]): Behavior[Message] = Behaviors.receive[Message](
    (ctx: ActorContext[Message], message: Message) => {
      import ctx.executionContext

      message match {
        case RefreshClientAuth(backoff) => {
          logger.debug("Refresh client auth")

          UnifiAuthorization()
            .onComplete({
              case Success(nextAuth) => ctx.self ! SetAuth(nextAuth)
              case Failure(exception) => {
                val nextTry: FiniteDuration = backoff * 2

                logger.error(s"Failed to login to unifi. Trying again in $nextTry", exception)

                timers.startSingleTimer(TimerKey, RefreshClientAuth(nextTry), nextTry)
              }
            })

          withState(maybeAuth, timers, pendingRequests)
        }
        case SetAuth(nextAuth) => {
          // respond to all our pending requests
          processPendingRequests(pendingRequests, nextAuth)

          // schedule our next auth refresh
          val nextRefresh: FiniteDuration = nextAuth.sessionCookie.expires
            .map((expiration: DateTime) => {
              (expiration.clicks - System.currentTimeMillis) milliseconds
            })
            .getOrElse(DefaultRefreshInterval)

          logger.debug(s"Setting new auth for ${nextAuth.username}, scheduling refresh in $nextRefresh")

          timers.startSingleTimer(TimerKey, RefreshClientAuth(), nextRefresh)

          withState(Some(nextAuth), timers, Seq())
        }
        case request: UnifiRequest[_] => {
          maybeAuth
            .map((auth: UnifiAuthorization) => {
              processRequest(request, auth)

              Behaviors.same[Message]
            })
            .getOrElse({
              // we don't have any auth so refresh it and make our request again
              ctx.self ! RefreshClientAuth()

              withState(maybeAuth, timers, pendingRequests :+ request)
            })
        }
      }
    }
  )
  */

  private def processPendingRequests(pendingRequest: Seq[UnifiRequest[_]], auth: UnifiAuthorization)
                                    (implicit ec: ExecutionContext): Unit = {
    pendingRequest.foreach(processRequest(_, auth))
  }

  private def processRequest[T](request: UnifiRequest[T], auth: UnifiAuthorization)
                               (implicit ec: ExecutionContext): Unit = {
    logger.debug(s"Sending request: $request")

    request.request(auth)
      .onComplete(request.ref ! _)
  }
}
