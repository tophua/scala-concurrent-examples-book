package ch5

import org.learningconcurrency._
import ch5._

import ParNonParallelizableOperations._



import scala.collection._
import scala.collection.par._
import scala.collection.par.Scheduler.Implicits.global


/**
 * Blitz(闪电)Comparison(比较)
 */
object BlitzComparison extends App {
  val array = (0 until 100000).toArray
  @volatile var x = 0
 //线性
  val seqtime = warmedTimed(1000) {
    array.reduce(_ + _)
  }
  //par标准 
  val partime = warmedTimed(1000) {
    array.par.reduce(_ + _)
  }
  //闪电m并发
  val blitztime = warmedTimed(1000) {
    x = array.toPar.reduce(_ + _)
  }

  log(s"sequential time - $seqtime")
  log(s"parallel time   - $partime")
  log(s"ScalaBlitz time - $blitztime")
}


object BlitzHierarchy extends App {
  val array = (0 until 100000).toArray
  val range = 0 until 100000

  def sum(xs: Zippable[Int]): Int = {
    xs.reduce(_ + _)
  }

  println(sum(array.toPar))

  println(sum(range.toPar))

}



