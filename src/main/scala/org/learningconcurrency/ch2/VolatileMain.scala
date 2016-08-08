package ch2
/**
 *
 */
object VolatileMain {

  def log(msg: String) {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  def thread(body: => Unit): Thread = {
    val t = new Thread {
      override def run() = body
    }
    t.start()
    t
  }
  def main(args: Array[String]): Unit = {
    /**
     * 下面程序查明几页文本中是否会至少含有一个!符号,几个独立线程开始扫描由一个人撰写的几页英雄故事
     * 一旦某个线程找到!符号,我们就需要停止其他线程的搜索操作
     */
    case class Page(txt: String, var position: Int)
    val pages = for (i <- 1 to 5) yield new Page("Na" * (100 - 20 * i) + " Batman!", -1)
    @volatile var found = false //代表某个线程已经找到感叹号
    for (p <- pages) yield thread {
      var i = 0
      while (i < p.txt.length && !found)
        if (p.txt(i) == '!') {
          p.position = i
          found = true
        } else i += 1
    }
    while (!found) {}
    log(s"results: ${pages.map(_.position)}")

  }
}