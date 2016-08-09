package ch4

import org.learningconcurrency._
/**
 * Future 它是一个容器类型，代表一个代码最终会返回的T类型结果。不过，代码可能会出错或执行超时，
 * 所以当Future完成时，它有可能完全没有被成功执行，这时它会代表一个异常
 *  Future 表示一个可能还没有实际完成的异步任务的结果,
 *  针对这个结果可以添加 Callback 以便在任务执行成功或失败后做出对应的操作
 */
object FuturesComputation extends App {
  /**
   * Computation计算
   *  Futures 执行计算
   * 1,首先引入global执行上下文,可以确保在全局上下文中执行Future计算
   * 调用log方法的次序不是确定,后跟代码块的Futures单例对象,是为调用Apply方法而添加语法糖
   * 
   */
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  Future {
    log(s"the future is here")
  }

  log(s"the future is coming")

}

/**
 *  通过Futures计算,使用Source.fromFile对象读取build.sbt文件内容
 * 1,首先引入global执行上下文,可以确保在全局上下文中执行Future计算
 * 
 * 
 */
object FuturesDataType extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source
 //通过Futures计算,使用Source.fromFile对象读取build.sbt文件内容
  val buildFile: Future[String] = Future {
    val f = Source.fromFile("build.sbt")
    try f.getLines.mkString("\n") finally f.close()
  }

  log(s"started reading build file asynchronously")
  /**
   * main线程会调用Future对象中的isCompleted方法,该Future对象为通过执行Future计算获得buildFile对象
   * 读取build.sbt文件的操作很可能很快完成,因此isCompleted方法会返回false,
   * 过250毫秒后,main线程会再次调用isCompleted方法会返回true,
   * 最后main线程会调用value方法,该方法会返回build.sb文件的内容
   * 
   */
  log(s"status: ${buildFile.isCompleted}")//是否完成
  Thread.sleep(250)
  log(s"status: ${buildFile.isCompleted}")//是否完成
  log(s"status: ${buildFile.value}")//还回值

}
/**
 * Futures对象的回调函数
 * 从w3网中查找出所有单词telnet.
 */

object FuturesCallbacks extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source
/**
 * 从w3网站获得url规范的文档,使用Source对象存储该文件的内容,并使用getUrlSpec文件中的Future对象
 * 以异步方式执行Http请求操作,getUrlSpec方法会先调用fromURL获取含有文本文档的Source对象,然后会调用
 * getLines方法获取文档中的行列表.
 */
  
  def getUrlSpec(): Future[Seq[String]] = Future {    
    val f = Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt")
    try {
      f.getLines.toList
      }finally{
      f.close()
     }
  }

  val urlSpec: Future[Seq[String]] = getUrlSpec()
/**
 * 要在Future对象urlSpec中找到包含有单词telnet的行,可以使用find方法
 * 该方法将行的列表和需要搜索的单词接收为参数,并且返回含有匹配内容的字符串.
 * 接收一个Seq类型的参数,而urlSpec对象返回Future[Seq[String]],因此无法将Future对象urlSpec
 * 直接发送给find方法,而且在程序调用find方法时,该Future很可能还无法使用.
 */
  def find(lines: Seq[String], word: String) = lines.zipWithIndex.collect {
    case (line, n) if line.contains(word) => (n, line)
  }.mkString("\n")
/**
 * 我们使用foreach方法为这个Future添加一个回调函数,注意onSuccess方法与foreach方法等价,但onSuccess方法可能
 * 会在scala 2.11之后被弃用,foreach方法接收偏函数作为其参数
 * 此处的要点是:添加回调函数是非阻塞操作,回调用函数注册后,main线程中用的log语句会立即执行
 * 但是执行回调函数中log语句的时间可以晚得多
 * 在Future对象被完善后,无须立刻调用回调参数,大多数执行上下文通过调用任务,以异步方式处理回调函数
 */
  urlSpec.foreach {
    lines => log(s"Found occurrences of 'telnet'\n${find(lines, "telnet")}\n")
  }
   Thread.sleep(2000)
   log("callbacks registered, continuing with other work")

/**
 * ForkJoinPool-1-worker-5: Found occurrences of 'telnet'
 * (207,  telnet , rlogin and tn3270 )
 * (745,                         nntpaddress | prosperoaddress | telnetaddress)
 * (806,  telnetaddress           t e l n e t : / / login )
 * (931,   for a given protocol (for example,  CR and LF characters for telnet)
   ForkJoinPool-1-worker-5: Found occurrences of 'password'
 * (107,                         servers). The password, is present, follows)
 * (109,                         the user name and optional password are)
 * (111,                         user of user name and passwords which are)
 * (222,      User name and password)
 * (225,   password for those systems which do not use the anonymous FTP)
 * (226,   convention. The default, however, if no user or password is)
 * (234,   is "anonymous" and the password the user's Internet-style mail)
 * (240,   currently vary in their treatment of the anonymous password.  )
 * (816,  login                   [ user [ : password ] @ ] hostport )
 * (844,  password                alphanum2 [ password ] )
 * (938,   The use of URLs containing passwords is clearly unwise. )
 */
  urlSpec.foreach {
    lines => log(s"Found occurrences of 'password'\n${find(lines, "password")}\n")
  }
  Thread.sleep(1000)

  log("callbacks installed, continuing with other work")
  

}

/**
 * Failure 回调
 */
object FuturesFailure extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val urlSpec: Future[String] = Future {
    Source.fromURL("http://www.w3.org/non-existent-url-spec.txt").mkString
  }

  urlSpec.failed.foreach {    
    case t => {      
      log(s"exception occurred - $t")     
    }    
  }

}


object FuturesExceptions extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val file = Future { Source.fromFile(".gitignore-SAMPLE").getLines.mkString("\n") }

  file foreach {
    text => log(text)
  }

  file.failed foreach {//异常处理,抛出异常类型FileNotFoundException
    case fnfe: java.io.FileNotFoundException => log(s"Cannot find file - $fnfe")
    case t => log(s"Failed due to $t")
  }

  import scala.util.{Try, Success, Failure}

  file onComplete {
    case Success(text) => log(text)
   //onComplete 回调方式
    case Failure(t) => log(s"Failed due to $t")
  }

}


object FuturesTry extends App {
  import scala.util._

  val threadName: Try[String] = Try(Thread.currentThread.getName)
  val someText: Try[String] = Try("Try objects are created synchronously")
  val message: Try[String] = for {
    tn <- threadName
    st <- someText
  } yield s"$st, t = $tn"

  message match {
    case Success(msg) => log(msg)
    case Failure(error) => log(s"There should be no $error here.")
  }

}


object FuturesNonFatal extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val f = Future { throw new InterruptedException }
  val g = Future { throw new IllegalArgumentException }
  f.failed foreach { case t => log(s"error - $t") }
  g.failed foreach { case t => log(s"error - $t") }
}


object FuturesClumsyCallback extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import org.apache.commons.io.FileUtils._
  import java.io._
  import scala.io.Source
  import scala.collection.convert.decorateAsScala._

  def blacklistFile(filename: String) = Future {
    val lines = Source.fromFile(filename).getLines
    lines.filter(!_.startsWith("#")).toList
  }
  
  def findFiles(patterns: List[String]): List[String] = {
    val root = new File(".")
    for {      
      f <- iterateFiles(root, null, true).asScala.toList
      pat <- patterns
      abspat = root.getCanonicalPath + File.separator + pat
      if f.getCanonicalPath.contains(abspat)
    } yield f.getCanonicalPath
  }

  blacklistFile(".gitignore") foreach {
    case lines =>
      val files = findFiles(lines)
      log(s"matches: ${files.mkString("\n")}")
  }
}


object FuturesMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source
  import scala.util.Success

  val buildFile = Future { Source.fromFile("build.sbt").getLines }
  val gitignoreFile = Future { Source.fromFile(".gitignore-SAMPLE").getLines }

  val longestBuildLine = buildFile.map(lines => lines.maxBy(_.length))
  val longestGitignoreLine = for (lines <- gitignoreFile) yield lines.maxBy(_.length)

  longestBuildLine onComplete {
    case Success(line) => log(s"the longest line is '$line'")
  }

  longestGitignoreLine.failed foreach {
    case t => log(s"no longest line, because ${t.getMessage}")
  }
}


object FuturesFlatMapRaw extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val netiquette = Future { Source.fromURL("http://www.ietf.org/rfc/rfc1855.txt").mkString }
  val urlSpec = Future { Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt").mkString }
  val answer = netiquette.flatMap { nettext =>
    urlSpec.map { urltext =>
      "First, read this: " + nettext + ". Now, try this: " + urltext
    }
  }

  answer foreach {
    case contents => log(contents)
  }
}


object FuturesFlatMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val netiquette = Future { Source.fromURL("http://www.ietf.org/rfc/rfc1855.txt").mkString }
  val urlSpec = Future { Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt").mkString }
  val answer = for {
    nettext <- netiquette
    urltext <- urlSpec
  } yield {
    "First of all, read this: " + nettext + " Once you're done, try this: " + urltext
  }

  answer foreach {
    case contents => log(contents)
  }

}


object FuturesDifferentFlatMap extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val answer = for {
    nettext <- Future { Source.fromURL("http://www.ietf.org/rfc/rfc1855.txt").mkString }
    urltext <- Future { Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt").mkString }
  } yield {
    "First of all, read this: " + nettext + " Once you're done, try this: " + urltext
  }

  answer foreach {
    case contents => log(contents)
  }

}


object FuturesRecover extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  import scala.io.Source

  val netiquetteUrl = "http://www.ietf.org/rfc/rfc1855.doc"
  val netiquette = Future { Source.fromURL(netiquetteUrl).mkString } recover {
    case f: java.io.FileNotFoundException =>
      "Dear boss, thank you for your e-mail." +
      "You might be interested to know that ftp links " +
      "can also point to regular files we keep on our servers."
  }

  netiquette foreach {
    case contents => log(contents)
  }

}


object FuturesReduce extends App {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val squares = for (i <- 0 until 10) yield Future { i * i }
  val sumOfSquares = Future.reduce(squares)(_ + _)

  sumOfSquares foreach {
    case sum => log(s"Sum of squares = $sum")
  }
}



