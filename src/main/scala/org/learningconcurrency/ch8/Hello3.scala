package org.learningconcurrency.ch8

import akka.actor.{ActorRef, ActorSystem, Props}


/**
  * Created by liush on 17-7-28.
  */
import akka.actor._
import akka.util._

/** Hellos with properties passed to constructor, using messages to communicate. */
object Hello3 extends App {
  /**
    * 如果尝试运行该代码几次，可能会看到这些行的顺序是相反的。这种排序是 Akka actor 系统动态本质的另一个例子，其中处理各个消息时的顺序是不确定的
    */
  import Greeter._
  val system = ActorSystem("actor-demo-scala")
  /**
    * Akka 使用 Props 对象将各种配置属性传递给 actor。每个 Props 实例包装 actor 类所需的构造函数参数的一个副本，
    * 以及对该类的引用。可通过两种方式将此信息传递给 Props 构造函数。
    * actor 的构造函数作为一个名称传递 (pass-by-name) 参数传递给 Props 构造函数。注意，此方式不会直接调用构造函数并传递结果；它传递构造函数调用
    */
  val bob = system.actorOf(props("Bob", "Howya doing"))
  val alice = system.actorOf(props("Alice", "Happy to meet you"))
  //Greeter actor 通过向另一个 actor 发送一条单独的消息来完成此任务：AskName 消息。AskName 消息本身不含任何信息，
  // 但收到它的 Greeter 实例知道应使用一个包含 TellName 发送方名称的 TellName 消息作为响应。当
  // 第一个 Greeter 收到所返回的 TellName 消息时，它打印出自己的问候语。
  bob ! Greet(alice)
  alice ! Greet(bob)
  Thread sleep 1000
  system shutdown

  object Greeter {
    //属性和交互
    case class Greet(peer: ActorRef)
    case object AskName
    case class TellName(name: String)
    def props(name: String, greeting: String) = Props(new Greeter(name, greeting))
  }
  /**
    * 真正的 actor 系统会使用多个 actor 来完成工作，它们彼此发送消息来进行交互
    *
    * @param myName
    * @param greeting
    */
  class Greeter(myName: String, greeting: String) extends Actor {
    import Greeter._
    def receive = {
      //属性和交互
      //Greeter actor 通过向另一个 actor 发送一条单独的消息来完成此任务：AskName 消息。AskName 消息本身不含任何信息，
      // 但收到它的 Greeter 实例知道应使用一个包含 TellName 发送方名称的 TellName 消息作为响应。当
      // 第一个 Greeter 收到所返回的 TellName 消息时，它打印出自己的问候语。
      case Greet(peer) => peer ! AskName
        //
      case AskName => sender ! TellName(myName)
      case TellName(name) => println(s"$greeting, $name")
    }
  }
}