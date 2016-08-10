package org.learningconcurrency

package object ch5 {
  @volatile var dummy: Any = _  
/**
 * 该方法会接收代码块,并返回执行代码主体所花费的时候
 */
  def timed[T](body: =>T): Double = {
    val start = System.nanoTime //记录当前时间
    dummy = body //调用代码块
    /**
     * JVM中的某些运行时优化技术(如死代码消除),可能会去除调用body代码块的语句,
     * 为了避免出现这种情况,将代码块body的返回值,赋予名为dummy的@volatile字段
     */
    val end = System.nanoTime //记录执行完body后的时候
    ((end - start) / 1000) / 1000.0 //计算两个时间的差值
  }

  def warmedTimed[T](times: Int = 200)(body: =>T): Double = {
    for (_ <- 0 until times) body
    timed(body)
  }
}

