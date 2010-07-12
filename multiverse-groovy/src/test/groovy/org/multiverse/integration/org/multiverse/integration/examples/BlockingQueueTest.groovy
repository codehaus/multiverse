package org.multiverse.integration.org.multiverse.integration.examples

import org.multiverse.integration.org.multiverse.integration.examples.BlockingQueue
import org.multiverse.api.exceptions.RetryTimeoutException

/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 12 Jul, 2010
 * Time: 9:55:06 PM
 * To change this template use File | Settings | File Templates.
 */

public class BlockingQueueTest extends GroovyTestCase {

  void testShouldBlockEnqueueWhenFull() {
    def queue = new BlockingQueue(maxSize: 2) //TODO: below size two TransactionList seems throw SpeculativeCOnfiguration Exception

    queue.enqueue("Hello")
    queue.enqueue("Hello")

    shouldFail(RetryTimeoutException) {
      queue.enqueue("Hello")
    }

  }

  void testShouldBlockDequeueWhenEmpty() {
    def queue = new BlockingQueue(maxSize: 2)

    shouldFail(RetryTimeoutException) {
      queue.dequeue()
    }

  }

}