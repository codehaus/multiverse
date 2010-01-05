package org.multiverse.integration.scala.examples

import org.multiverse.integration.scala.StmUtils._;
import org.multiverse.transactional.primitives.manual.Ref;

/**
 * A &quot;port&quot; of the Java {@link org.multiverse.integrationtests.classicproblems.DiningPhilosophersLongTest DiningPhilosophers} 
 * example to Scala.
 *
 * @author Andrew Phillips
 */
class DiningPhilosophers {
    val philosopherCount = 10
    val eatCount = 1000

    lazy val forks: List[Ref[PhilosopherThread]] = createForks
    lazy val philosopherThreads: List[PhilosopherThread] = createPhilosopherThreads

    private[examples] def run() {
        philosopherThreads.foreach(_.start())
        philosopherThreads.foreach(_.join())
    
        forks.foreach(f =>
            if (!f.isNull) throw new AssertionError(
                String.format("Fork %s is being held by Philosopher %s", f, f.get))
        )
    }

    private def createForks = 
        for (k <- List.range(0, philosopherCount)) yield new Ref[PhilosopherThread] 
    
    private def createPhilosopherThreads =
        for {
            k <- List.range(0, philosopherCount)
            leftFork = forks(k)
            rightFork = forks((k + 1) % philosopherCount)
        } yield new PhilosopherThread(k, leftFork, rightFork)

    abstract class ExampleThread(name: String) extends Thread(name) {
        override def run() {
            try {
                doRun()
            } catch {
                case ex => {
                    println(String.format("Thread %s has thrown an exception\n", name))
                    ex.printStackTrace()
                }
            }
        }

        def doRun()
    }
    
    class PhilosopherThread(id: Int, leftFork: Ref[PhilosopherThread], rightFork: Ref[PhilosopherThread]) 
        extends ExampleThread("PhilosopherThread-" + id) {

        def doRun() {
            for (k <- List.range(0, eatCount)) {
                if (k % 100 == 0) println(String.format("%s at %s\n", getName, k.toString))
                eat()
            }
        }

        private def eat() {
            takeForks()
            stuffHole()
            releaseForks()
        }

        private def takeForks() {
            atomic {
                if (!leftFork.isNull) retry()
                else leftFork.set(this)
                
                if (!rightFork.isNull) retry()
                else rightFork.set(this)
            }
        }

        private def stuffHole() {
            //simulate the eating
            Thread.sleep((Math.random * 50).asInstanceOf[Long])
            Thread.`yield`()
        }

        private def releaseForks() {
            atomic {
                leftFork.clear()
                rightFork.clear()
            }
        }
    }
}


/*
 * The 'main' method can't be defined on the companion object - see bug 
 * http://lampsvn.epfl.ch/trac/scala/ticket/363.
 */ 
object DiningPhilosophersRunner {
    import org.multiverse.api.GlobalStmInstance
    import org.multiverse.stms.alpha.AlphaStm
    
    def main(args: Array[String]) {
        new DiningPhilosophers().run()
    }
}