package ch3

import org.learningconcurrency._
import ch3._

object CollectionsBad extends App {
  import scala.collection._

  val buffer = mutable.ArrayBuffer[Int]()

  def add(numbers: Seq[Int]) = execute {
    buffer ++= numbers
    log(s"buffer = $buffer")
  }

  add(0 until 10)
  add(10 until 20)
}


object CollectionsSynchronized extends App {
  import scala.collection._

  val buffer = new mutable.BufferProxy[Int] with mutable.SynchronizedBuffer[Int] {
    val self = mutable.ArrayBuffer[Int]()
  }

  execute {
    buffer ++= (0 until 10)
    log(s"buffer = $buffer")
  }

  execute {
    buffer ++= (10 until 20)
    log(s"buffer = $buffer")
  }

}


object MiscSyncVars extends App {
  import scala.concurrent._
  val sv = new SyncVar[String]

  execute {
    Thread.sleep(500)
    log("sending a message")
    sv.put("This is secret.")
  }

  log(s"get  = ${sv.get}")
  log(s"take = ${sv.take()}")

  execute {
    Thread.sleep(500)
    log("sending another message")
    sv.put("Secrets should not be logged!")
  }

  log(s"take = ${sv.take()}")
  log(s"take = ${sv.take(timeout = 1000)}")
}


object MiscDynamicVars extends App {
  import scala.util.DynamicVariable

  val dynlog = new DynamicVariable[String => Unit](log)
  def secretLog(msg: String) = println(s"(unknown thread): $msg")

  execute {
    dynlog.value("Starting asynchronous execution.")
    dynlog.withValue(secretLog) {
      dynlog.value("Nobody knows who I am.")
    }
    dynlog.value("Ending asynchronous execution.")
  }

  dynlog.value("is calling the log method!")
}


object CollectionsIterators extends App {
  import java.util.concurrent._

  val queue = new LinkedBlockingQueue[String]
  //一个队列就是一个先入先出（FIFO）的数据结构,
  //如果想在一个满的队列中加入一个新项，多出的项就会被拒绝,
  //这时新的 offer 方法就可以起作用了。它不是对调用 add() 方法抛出一个 unchecked 异常，而只是得到由 offer() 返回的 false
  for (i <- 1 to 5500) queue.offer(i.toString)
  execute {
    val it = queue.iterator
    while (it.hasNext) log(it.next())
  }
  for (i <- 1 to 5500) queue.poll()
}


object CollectionsConcurrentMap extends App {
  import java.util.concurrent.ConcurrentHashMap
  import scala.collection._
  import scala.collection.convert.decorateAsScala._
  import scala.annotation.tailrec

  val emails = new ConcurrentHashMap[String, List[String]]().asScala

  execute {
    emails("James Gosling") = List("james@javalove.com")
    log(s"emails = $emails")
  }

  execute {
    emails.putIfAbsent("Alexey Pajitnov", List("alexey@tetris.com"))
    log(s"emails = $emails")
  }

  execute {
    emails.putIfAbsent("Alexey Pajitnov", List("alexey@welltris.com"))
    log(s"emails = $emails")
  }

}


object CollectionsConcurrentMapIncremental extends App {
  import java.util.concurrent.ConcurrentHashMap
  import scala.collection._
  import scala.collection.convert.decorateAsScala._
  import scala.annotation.tailrec

  val emails = new ConcurrentHashMap[String, List[String]]().asScala

  @tailrec def addEmail(name: String, address: String) {
    emails.get(name) match {
      case Some(existing) =>
        //
        if (!emails.replace(name, existing, address :: existing)) addEmail(name, address)
      case None =>
        //putIfAbsent()方法用于在 map 中进行添加,这个方法以要添加到 ConcurrentMap实现中的键的值为参数，就像普通的 put() 方法，
        //但是只有在 map 不包含这个键时，才能将键加入到 map 中。如果 map 已经包含这个键，那么这个键的现有值就会保留。
        if (emails.putIfAbsent(name, address :: Nil) != None) addEmail(name, address)
    }
  }

  execute {
    addEmail("Yukihiro Matsumoto", "ym@ruby.com")
    log(s"emails = $emails")
  }

  execute {
    addEmail("Yukihiro Matsumoto", "ym@ruby.io")
    log(s"emails = $emails")
  }

}


object CollectionsConcurrentMapBulk extends App {
  import scala.collection._
  import scala.collection.convert.decorateAsScala._
  import java.util.concurrent.ConcurrentHashMap

  val names = new ConcurrentHashMap[String, Int]().asScala
  names("Johnny") = 0
  names("Jane") = 0
  names("Jack") = 0

  execute {
    for (n <- 0 until 10) names(s"John $n") = n
  }

  execute {
    for (n <- names) log(s"name: $n")
  }

}


object CollectionsTrieMapBulk extends App {
  import scala.collection._

  val names = new concurrent.TrieMap[String, Int]
  names("Janice") = 0
  names("Jackie") = 0
  names("Jill") = 0

  execute {
    for (n <- 10 until 100) names(s"John $n") = n
  }

  execute {
    log("snapshot time!")
    for (n <- names.map(_._1).toSeq.sorted) log(s"name: $n")
  }

}





