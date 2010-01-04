package org.multiverse.integration.scala

import org.junit.runner.RunWith
import org.multiverse.api.exceptions.RetryError
import org.multiverse.api.ThreadLocalTransaction._
import org.multiverse.transactional.primitives.manual.Ref
import org.multiverse.integration.scala.StmUtils._
import org.multiverse.stms.alpha.AlphaStm
import org.multiverse.stms._
import org.multiverse.utils.clock.StrictClock
import org.scalatest.Spec
import org.scalatest.junit.JUnitRunner

/**
 * Unit tests for the {@code StmUtils} class.
 *
 * @author Andrew Phillips
 */
@RunWith(classOf[JUnitRunner])
class StmUtilsSpec extends Spec {
     
    describe("retry") {
        it("throws a RetryError") {
            intercept[RetryError] {
                setThreadLocalTransaction(
                    new AbstractTransaction(new AbstractTransactionDependencies(), null) {})
                retry()
            }            
        }
    }

    describe("atomic") {
        it("will execute a simple block when invoked") {
            val intVal: Ref[Int] = Ref.createCommittedRef(0)
            atomic {
                intVal.set(1)
            }
            assert(intVal.get === 1)
        }
        it("will execute only the either block if successful") {
            val intVal: Ref[Int] = Ref.createCommittedRef(0)
            atomic {
                {
                    intVal.set(1)
                } orelse {
                    intVal.set(2)
                }
            }
            assert(intVal.get === 1)
        }     
        it("will execute the orelse block if the either block fails") {
            val intVal: Ref[Int] = Ref.createCommittedRef(0)
            atomic {
                {
                    intVal.set(1)
                    retry()
                } orelse {
                    intVal.set(2)
                }
            }
            assert(intVal.get === 2)
        }
    }

    describe("StmEither.orelse") {
        it("cannot be evaluated outside the scope of a transaction") {
            intercept[NullPointerException] {
                new StmEither({ 1 }).orelse({ 2 })   
            }
        }        
    }
}