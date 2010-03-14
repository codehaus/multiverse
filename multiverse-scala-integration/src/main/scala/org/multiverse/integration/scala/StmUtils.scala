package org.multiverse.integration.scala

import org.multiverse.api.{StmUtils => JavaStmUtils, Transaction}
import org.multiverse.templates.{TransactionTemplate, OrElseTemplate}

/**
 * Contains a set of utility functions and syntax convenience to integrate 
 * Multiverse with Scala.
 * <p>
 * Usage:
 * <pre>
 * import org.multiverse.integration.scala.StmUtils._
 *
 * // execute 'block' atomically
 * atomic  {
 *   block
 * }
 *
 * // atomically try to execute 'block1', if that fails, 'block2'
 * atomic  {
 * {
 *     block1
 * } orelse  {
 *     block2
 * }
 * }
 *
 * // forces a retry of the current atomic block
 * atomic  {
 *   ...
 *   retry()
 * }
 * </pre>
 *
 * @author Peter Veentjer
 * @author Andrew Phillips
 * @see org.multiverse.templates.TransactionTemplate
 * @see org.multiverse.templates.OrElseTemplate
 * @see org.multiverse.api.StmUtils # retry ( )
 */
object StmUtils {
  /**
   * Requests an abort and retry of the currently executing transaction.
   * See  { @link org.multiverse.api.StmUtils # retry ( ) StmUtils#retry() }.
   */
  def retry() {
    JavaStmUtils.retry()
  }

  /**
   * Wraps a statement block in an  { @link AtomicTemplate } and executes the template.
   * <p>
   * On a more academic note, this can be viewed as the <a href="http://en.wikipedia.org/wiki/Monad_%28functional_programming%29#Definition">
   * unit function</a> for an  { @code Atomic } monad, taking &quot;plain&quot; statements to
   * statements executed in an atomic context.
   *
   * @param block the statement block to be executed atomically
   * @return the value returned by  { @link AtomicTemplate # execute ( ) }
   */
  def atomic[E](block: => E) =
    new TransactionTemplate[E] {
      def execute(t: Transaction) = block
    }.execute()

  /**
   * &quot;Provides&quot; the  { @code orelse } method for the
   * <code> { block } orelse  { ... } </code> construction.
   */
  class StmEither[E](eitherBlock: => E) {

    /**
     *
     */
    def orelse(orelseBlock: => E) = {
      new OrElseTemplate[E] {
        def run(t: Transaction) = eitherBlock

        def orelserun(t: Transaction) = orelseBlock
      }.execute()
    }
  }

  /**
   * Allows  { @code block } in <code> { block } orelse  { ... } </code> to be lifted
   * to <code>new StmEither(block).orelse(...)</code>.
   *
   * @param block the statement block to be wrapped
   * @return an { @link StmEither } with  { @code block } as the 'either' block
   */
  implicit def blockToEither[E](block: => E) = new StmEither[E](block)
}    