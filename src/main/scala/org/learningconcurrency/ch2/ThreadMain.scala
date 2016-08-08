
package ch2

object ThreadMain {
  /**
   * 线程通信方式:彼此等待对方直到执行完毕,交出处理器控制权的线程发出的信息是它已经执行完毕.
   */
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
  def main(args: Array[String]) {
    //获得当前线程引用,并将该引用存储到名为t的局部变更中.
    val t = Thread.currentThread()
    //获得当前线程的名称 
    val name = t.getName
    println(s"I am the thread $name")

    class MyThread extends Thread {
      override def run(): Unit = {
        println("New thread running.")
      }
    }
    val tmy = new MyThread

    tmy.start()
    //将main线程切换到等待状态直到MyThread线程执行完毕为止
    tmy.join() //该方法会暂停main线程的执行过程,
    println("New thread joined.")
    val th = thread {
      Thread.sleep(1000)
      log("New thread running.")
      Thread.sleep(1000)
      log("Still running.")
      Thread.sleep(1000)
      log("Completed.")
    }
    th.join()
    log("New thread joined.")
    /**
     * 结论最后输出结果永远都是"New thread joined.",
     * 使用Thread类中的静态方法sleep,该方法可以暂停正在执行的线程
     * 使之在一段指定的时间(单位为毫秒)内等待,这个方法会使用该线程进入
     * 计时等待状态,当调用sleep方法时,OS可以将其他线程正在使用的处理器
     * 重新分配给其他线程.,join方法将main线程切换到了等待状态,直到变更
     * t中的新线程执行完毕为止.
     */

    /*for (i <- 0 until 100000) {
      var a = false
      var b = false
      var x = -1
      var y = -1
      
       // 多次运行这个程序,我们会得到下面的结果,该结果表明变量X和Y被同时赋予1了
       

      val t1 = thread {
        Thread.sleep(2)
        a = true
        y = if (b) 0 else 1
      }
      val t2 = thread {
        Thread.sleep(2)
        b = true
        x = if (a) 0 else 1
      }

      t1.join()
      t2.join()
      assert(!(x == 1 && y == 1), s"x = $x, y = $y")
    }*/

    import scala.collection._
    val transfers = mutable.ArrayBuffer[String]()
    /**
     * ArrayBuffer数组实现的是一个专门由单线程使用的集合,因此我们应该防止它被执行并发写入操作
     */
    def logTransfer(name: String, n: Int): Unit = transfers.synchronized {
      transfers += s"transfer to account '$name' = $n"
    }
    /**
     * 账号Account对象含有其所有者的信息以及他们拥有的资金数额
     */
    class Account(val name: String, var money: Int)
    /**
     * 向账号中充值,该系统会使用add方法获取指定Account对象的监控器
     * 并修改该对象中的money字段
     */
    def add(account: Account, n: Int) = account.synchronized { //获得transfers监控器
      account.money += n //如果转账金额超过10个货币单位,需要该操作记录下来
      if (n > 10) logTransfer(account.name, n)
    }
    val jane = new Account("Jane", 100)
    val john = new Account("John", 200)
    val t1 = thread { add(jane, 5) }
    val t2 = thread { add(john, 50) }
    val t3 = thread { add(jane, 70) }
    t1.join(); t2.join(); t3.join()
    log(s"--- transfers ---\n$transfers")
    
    
    import scala.collection._
  /**
 * 使用Queue存储被调度的代码块,使用函数() => Unit设置这些代码块
 */
   val tasks = mutable.Queue[() => Unit]()
   
  /**
 * 线程Worker反复调用poll方法,在变量task存储的对象中实现同步化,
 * 检查该对象代表的队列是否为空,poll方法展示了synchronized语句可以返回一个值
 */
  val worker = new Thread {
    def poll(): Option[() => Unit] = tasks.synchronized {
      //返回一个可选的Some值,否则该语句返回一个None,dequeue弹出队列
      if (tasks.nonEmpty) Some(tasks.dequeue()) else None
    }
   
    override def run() = while (true) poll() match {
      case Some(task) => task() //如果方法则调用
      case None =>
    }
  }
  //设置守护线程,设置守护线程的原因,synchronized方法向它发送任务,
  //该方法会调用指定的代码块,以便最终执行worker线程
  worker.setDaemon(true)
  //运行work线程
  worker.start()
  //设置守护线程的原因,synchronized方法向它发送任务,
  //该方法会调用指定的代码块,以便最终执行worker线程
  def asynchronous(body: =>Unit) = tasks.synchronized {
    tasks.enqueue(() => body)//插入队列
  }

  asynchronous { log("Hello") }//传递方法
  asynchronous { log(" world!")}//传递方法
  Thread.sleep(100)

  }

}

