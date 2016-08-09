package ch3


import org.learningconcurrency._
import ch3._

import java.io._
import java.util.concurrent._
import java.util.concurrent.atomic._
import scala.annotation.tailrec
import scala.collection._
import scala.collection.convert.decorateAsScala._
import org.apache.commons.io.FileUtils



object FileSystemTest extends App {
  val fileSystem = new FileSystem(".")

  fileSystem.logMessage("Testing log!")

  fileSystem.deleteFile("test.txt")

  fileSystem.copyFile("build.sbt", "build.sbt.backup")

  val rootFiles = fileSystem.filesInDir("")
  log("All files in the root dir: " + rootFiles.mkString(", "))
}


class FileSystem(val root: String) {

  val logger = new Thread {
    setDaemon(true)
    override def run() {
      while (true) {
        val msg = messages.take()
        log(msg)
      }
    }
  }

  logger.start()
/**
 * LinkedBlockingQueue 一个由链接节点支持的可选有界队列
 * 
 */
  private val messages = new LinkedBlockingQueue[String]

  def logMessage(msg: String): Unit = messages.add(msg)

  sealed trait State

  class Idle extends State

  class Creating extends State

  class Copying(val n: Int) extends State

  class Deleting extends State

  class Entry(val isDir: Boolean) {
    //AtomicReference则对应普通的对象引用,也就是它可以保证你在修改对象引用时的线程安全性。
    val state = new AtomicReference[State](new Idle)
  }

  val files: concurrent.Map[String, Entry] =
    //new ConcurrentHashMap().asScala
    new concurrent.TrieMap()
 //iterateFiles 第二个参数过滤器，第三个参数是否递归
  for (file <- FileUtils.iterateFiles(new File(root), null, false).asScala) {
    files.put(file.getName, new Entry(false))
  }

  @tailrec private def prepareForDelete(entry: Entry): Boolean = {
    val s0 = entry.state.get
    s0 match {
      case i: Idle =>
        /**
         * compareAndSet 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。
         * 参数：
         * expect - 预期值
         * update - 新值
         * 返回：如果成功，则返回 true。返回 false 指示实际值与预期值不相等。
         */

        
        if (entry.state.compareAndSet(s0, new Deleting)) true
        else prepareForDelete(entry)
      case c: Creating =>
        logMessage("File currently being created, cannot delete.")
        false
      case c: Copying =>
        logMessage("File currently being copied, cannot delete.")
        false
      case d: Deleting =>
        false
    }
  }

  def deleteFile(filename: String): Unit = {
    files.get(filename) match {
      case None =>
        logMessage(s"Cannot delete - path '$filename' does not exist!")
      case Some(entry) if entry.isDir =>
        logMessage(s"Cannot delete - path '$filename' is a directory!")
      case Some(entry) =>
        execute {
          if (prepareForDelete(entry)) {
            //安全删除
            if (FileUtils.deleteQuietly(new File(filename)))
              files.remove(filename)
          }
        }
    }
  }

  @tailrec private def acquire(entry: Entry): Boolean = {
    val s0 = entry.state.get
    s0 match {
      case _: Creating | _: Deleting =>
        logMessage("File inaccessible, cannot copy.")
        false
      case i: Idle =>
        if (entry.state.compareAndSet(s0, new Copying(1))) true
        else acquire(entry)
      case c: Copying =>
        /**
         * compareAndSet方法作用是首先检查当前引用是否等于预期引用，并且当前标志是否等于预期标志，如果全部相等，
         * 则以原子方式将该引用和该标志的值设置为给定的更新值
         */
        if (entry.state.compareAndSet(s0, new Copying(c.n + 1))) true
        else acquire(entry)
    }
  }

  @tailrec private def release(entry: Entry): Unit = {
    val s0 = entry.state.get
    s0 match {
      case i: Idle =>
        sys.error("Error - released more times than acquired.")
      case c: Creating =>
        if (!entry.state.compareAndSet(s0, new Idle)) release(entry)
      case c: Copying if c.n <= 0 =>
        sys.error("Error - cannot have 0 or less copies in progress!")
      case c: Copying =>
        val newState = if (c.n == 1) new Idle else new Copying(c.n - 1)
        if (!entry.state.compareAndSet(s0, newState)) release(entry)
      case d: Deleting =>
        sys.error("Error - releasing a file that is being deleted!")
    }
  }

  def copyFile(src: String, dest: String): Unit = {
    files.get(src) match {
      case None =>
        logMessage(s"File '$src' does not exist.")
      case Some(srcEntry) if srcEntry.isDir =>
        sys.error(s"Path '$src' is a directory!")
      case Some(srcEntry) =>
        execute {
          if (acquire(srcEntry)) try {
            val destEntry = new Entry(false)
            destEntry.state.set(new Creating)
            //putIfAbsent 返回与指定键关联的以前的值，或如果没有键映射None
            if (files.putIfAbsent(dest, destEntry) == None) try {
              //文件复制
              FileUtils.copyFile(new File(src), new File(dest))
            } finally release(destEntry)
          } finally release(srcEntry)
        }
    }
  }

  def filesInDir(dir: String): Iterable[String] = {
    // trie map snapshots
    for ((name, state) <- files; if name.startsWith(dir)) yield name
  }

}