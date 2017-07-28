
package ch8

import org.learningconcurrency._
import ch8._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.Logging
import akka.actor.Props
import akka.actor.ActorSystem
import scala.io.Source
import scala.collection._

/**
  * 这个问题的答案很简单：“不是”。Akka actor 是异步运行的,所以即使目标 actor 与发送方 actor 位于相同的 JVM 中,目标 actor 也绝不会立即开始执行。
  * 相反，处理该消息的线程会将消息添加到目标 actor 的邮箱中。将消息添加到邮箱中会触发一个线程,
  * 以便从邮箱获取该消息并调用 actor 的 receive 方法来处理。但从邮箱获取消息的线程通常不同于将消息添加到邮箱的线程。
  * @param hello
  */

class HelloActor(val hello: String) extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    //注意`使用方式,不是单引号,引用类hello属性
    case `hello` =>
      log.info(s"Received a '$hello'... $hello!")
      //mes any匹配任何类型
    case msg     =>
      log.info(s"Unexpected message '$msg'")
      context.stop(self)
  }
}


object HelloActor {
  //Akka 通过Props实例创建Actor
  def props(hello: String) = Props(new HelloActor(hello))
  def propsVariant(hello: String) = Props(classOf[HelloActor], hello)
}


object ActorsCreate extends App {
  //在系统内创建一个 actor,它为所创建的 actor 返回一个 actor 引用
  //它使用了专门用于 Hello actor 类型的配置属性,greeter 使用指定的名称创建此上下文中的子角色
  //HelloActor.props("hi")参数类型,
  val hiActor: ActorRef = ourSystem.actorOf(HelloActor.props("hi"), name = "greeter")
  //使用 actor 引用向 actor 发送消息
  hiActor ! "hi"
  //等待一秒钟，
  Thread.sleep(1000)
  hiActor ! "hola"
  Thread.sleep(1000)
  //然后关闭 actor 系统
  ourSystem.shutdown()
}


class DeafActor extends Actor {
  val log = Logging(context.system, this)
  def receive = PartialFunction.empty
  //unhandled方法使用
  override def unhandled(msg: Any) = msg match {
    case msg: String => log.info(s"could not handle '$msg'")
    case _           => super.unhandled(msg)
  }
}


object ActorsUnhandled extends App {
  val deafActor = ourSystem.actorOf(Props[DeafActor], name = "deafy")
  deafActor ! "hi"
  Thread.sleep(1000)
  deafActor ! 1234
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class CountdownActor extends Actor {
  var n = 10
  def counting: Actor.Receive = {
    case "count" =>
      n -= 1
      log(s"n = $n")
      //原子方法调用方式
      if (n == 0) context.become(done)
  }
  def done = PartialFunction.empty
  def receive = counting
}


object ActorsCountdown extends App {
  val countdown = ourSystem.actorOf(Props[CountdownActor])
  for (i <- 0 until 20) countdown ! "count"
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class DictionaryActor extends Actor {
  private val log = Logging(context.system, this)
  private val dictionary = mutable.Set[String]()
  def receive = uninitialized
  def uninitialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.Init(path) =>
      val stream = getClass.getResourceAsStream(path)
      val words = Source.fromInputStream(stream)
      for (w <- words.getLines) dictionary += w
      context.become(initialized)
  }
  def initialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.IsWord(w) =>
      log.info(s"word '$w' exists: ${dictionary(w)}")
    case DictionaryActor.End =>
      dictionary.clear()
      context.become(uninitialized)
  }
  override def unhandled(msg: Any) = {
    log.info(s"message $msg should not be sent in this state.")
  }
}


object DictionaryActor {
  case class Init(path: String)
  case class IsWord(w: String)
  case object End
}


object ActorsBecome extends App {
  val dict = ourSystem.actorOf(Props[DictionaryActor], "dictionary")
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.Init("/org/learningconcurrency/words.txt") // or /usr/share/dict/words 
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("balaban")
  Thread.sleep(1000)
  dict ! DictionaryActor.End
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("termination")
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class ParentActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "create" =>
      context.actorOf(Props[ChildActor])
      log.info(s"created a new child - children = ${context.children}")
    case "sayhi" =>
      log.info("Kids, say hi!")
      for (c <- context.children) c ! "sayhi"
    case "stop" =>
      log.info("parent stopping")
      context.stop(self)
  }
}


class ChildActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "sayhi" =>
      val parent = context.parent
      log.info(s"my parent $parent made me say hi!")
  }
  override def postStop() {
    log.info("child stopped!")
  }
}


object ActorsHierarchy extends App {
  val parent = ourSystem.actorOf(Props[ParentActor], "parent")
  parent ! "create"
  parent ! "create"
  Thread.sleep(1000)
  parent ! "sayhi"
  Thread.sleep(1000)
  parent ! "stop"
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class CheckActor extends Actor {
  import akka.actor.{Identify, ActorIdentity}
  val log = Logging(context.system, this)
  def receive = {
    case path: String =>
      log.info(s"checking path $path")
      context.actorSelection(path) ! Identify(path)
    case ActorIdentity(path, Some(ref)) =>
      log.info(s"found actor $ref on $path")
    case ActorIdentity(path, None) =>
      log.info(s"could not find an actor on $path")
  }
}


object ActorsIdentify extends App {
  val checker = ourSystem.actorOf(Props[CheckActor], "checker")
  checker ! "../*"
  Thread.sleep(1000)
  checker ! "../../*"
  Thread.sleep(1000)
  checker ! "/system/*"
  Thread.sleep(1000)
  checker ! "/user/checker2"
  Thread.sleep(1000)
  checker ! "akka://OurExampleSystem/system"
  Thread.sleep(1000)
  ourSystem.stop(checker)
  Thread.sleep(1000)
  ourSystem.shutdown()
}


class LifecycleActor extends Actor {
  val log = Logging(context.system, this)
  var child: ActorRef = _
  def receive = {
    case num: Double  => log.info(s"got a double - $num")
    case num: Int     => log.info(s"got an integer - $num")
    case lst: List[_] => log.info(s"list - ${lst.head}, ...")
    case txt: String  => child ! txt
  }
  override def preStart(): Unit = {
    log.info("about to start")
    child = context.actorOf(Props[StringPrinter], "kiddo")
  }
  override def preRestart(reason: Throwable, msg: Option[Any]): Unit = {
    log.info(s"about to restart because of $reason, during message $msg")
    super.preRestart(reason, msg)
  }
  override def postRestart(reason: Throwable): Unit = {
    log.info(s"just restarted due to $reason")
    super.postRestart(reason)
  }
  override def postStop() = log.info("just stopped")
}


class StringPrinter extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case msg => log.info(s"child got message '$msg'")
  }
  override def preStart(): Unit = log.info(s"child about to start.")
  override def postStop(): Unit = log.info(s"child just stopped.")
}


object ActorsLifecycle extends App {
  val testy = ourSystem.actorOf(Props[LifecycleActor], "testy")
  testy ! math.Pi
  Thread.sleep(1000)
  testy ! 7
  Thread.sleep(1000)
  testy ! "hi there!"
  Thread.sleep(1000)
  testy ! Nil
  Thread.sleep(1000)
  testy ! "sorry about that"
  Thread.sleep(1000)
  ourSystem.stop(testy)
  Thread.sleep(1000)
  ourSystem.shutdown()
}












