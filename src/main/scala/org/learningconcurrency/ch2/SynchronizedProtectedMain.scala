package ch2

object SynchronizedProtectedMain {
/**
 * 接收一个代码块主体,创建一个在其run方法中执行该代码块的新线程,运行这个新建线程,然后返回对该线程的引用,
 * 从而使用其他线程能够调用该线程中的join方法
 */
  def thread(body: =>Unit): Thread = {
    val t = new Thread {
      override def run() = body
    }
    t.start()
    t
  }
  
  def main(args: Array[String]): Unit = {

    var result:String=null
    val t=thread{result="\n title\n"+"="*5}
    t.join()
    /**
     * result永远不会显示null,因为调用join方法的操作总是在调用println方法之前执行,而且对变量result执行的
     *join() //该方法会暂停main线程的执行过程,将main线程切换到等待状态直到MyThread线程执行完毕为止
     */
    println(s"${Thread.currentThread.getName}: $result")
    
    var uidCount = 0L
   /**
    * synchronized 语句确保一个线程执行的同步化代码不会同时再由其他线程执行,
    * 还确保了同一对象(this)中的其他同步代码块不会被调用
    */
    def getUniqueIdSyn() = this.synchronized {
      val freshUid = uidCount + 1
      uidCount = freshUid
      freshUid
    }
    /**
     * 是由不同独立 线程中相应语句的执行次序决定的,
     * 变量t中的线程有时会以并发方式调用,getUniqueId方法,在程序开头以以并发方式读取变量uidCount的值
     * 该变量的初始化值为0,然后它们推断自己的变量freshUid为值1,freshUid是一个局部变量,因此它获得的是线程内存
     * 所有线程都能够看到该变量的独立实例.
     * 
     */
    def getUniqueId()={
      val freshUid = uidCount + 1
      uidCount = freshUid
      freshUid
    }
    //
    def printUniqueIds(n: Int): Unit = {
      val uids = for (i <- 0 until n) yield getUniqueIdSyn()
      println(s"Generated uids: $uids")
    }
    /** 
     *  添加synchronized代码
     * Generated uids: Vector(2, 3, 5, 8, 10)
     * Generated uids: Vector(1, 4, 6, 7, 9)
     * 
     * 未加synchronized代码
     * Generated uids: Vector(1, 3, 4, 5, 6)
		 * Generated uids: Vector(2, 4, 7, 8, 9)		
     */
    val th = thread {
      printUniqueIds(5)
    }
    printUniqueIds(5)
    th.join()
    
    
     for (i <- 0 until 10000) {
    var t1started = false
    var t2started = false
    var t1index = 0
    var t2index = 0
  //下面两个线程(t1,t2)访问一对布尔型变量(a和b)和一对变量(x,y)
  //线程t1将变量a设置为true,然后记取变量b的值,如果变量b的值为true
  //线程t1就会将0赋予变量y,否则就会将1赋予变量y
    val t1 = thread {
      Thread.sleep(1)
      this.synchronized { t1started = true }
      //t1started = true
      val t2s = this.synchronized { t2started }
      t2index = if (t2started) 0 else 1
    }
    val t2 = thread {
      Thread.sleep(1)
       //t2started = true
      this.synchronized { t2started = true }
      val t1s = this.synchronized { t1started }
      t1index = if (t1s) 0 else 1
    }
  
    t1.join()
    t2.join()
    assert(!(t1index == 1 && t2index == 1), s"t1 = $t1index, t2 = $t2index")
  }

  }
}