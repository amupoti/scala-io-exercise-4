package com.xebia
package exercise4

import scala.concurrent.duration._
import scala.util.{Success, Failure}

import akka.actor.{Actor, Props}

import ReverserFactory.AsyncReverseFunction

object ReverseActor {
  def props = Props[ReverseActor]
  def name = "reverser"

  sealed trait Result
  case class Reverse(value:String)

  case class ReverseResult(value:String) extends Result

  case object PalindromeResult extends Result

  case object Init

  case object NotInitialized extends Result
}

class ReverseActor extends Actor {
  import ReverseActor._

  import context._

  self ! Init

  def receive = uninitialized

  def uninitialized:Receive = {
    case Init => ReverserFactory.loadReverser.onComplete {
      case Success(reverse) => context.become(initialized(reverse))
      case Failure(e)       => context.system.scheduler.scheduleOnce(1 seconds, self, Init)
    }
    case Reverse(value) => sender ! NotInitialized
  }

  def initialized(reverse: AsyncReverseFunction):Receive = {
    case Reverse(value) =>
      val theSender = sender

      reverse(value).map { reversed =>
        if(reversed == value) theSender ! PalindromeResult
        else theSender ! ReverseResult(reversed)
      }
  }
}
