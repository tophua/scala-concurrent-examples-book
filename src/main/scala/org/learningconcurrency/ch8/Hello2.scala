package org.learningconcurrency.ch8

import akka.actor.{Actor, ActorSystem, Props}

/**
  * Created by liush on 17-7-28.
  */
object Hello2 extends App {

  case class Greeting(greet: String)
  case class Greet(name: String)

  val system = ActorSystem("actor-demo-scala")
  val hello = system.actorOf(Props[Hello], "hello")
  hello ! Greeting("Hello")
  hello ! Greet("Bob")
  hello ! Greet("Alice")
  hello ! Greeting("Hola")
  hello ! Greet("Alice")
  hello ! Greet("Bob")
  Thread sleep 1000
  system shutdown

  class Hello extends Actor {
    //Actor 和状态
    var greeting = ""
    def receive = {
      case Greeting(greet) => greeting = greet
      case Greet(name) => println(s"$greeting $name")
    }
  }
}