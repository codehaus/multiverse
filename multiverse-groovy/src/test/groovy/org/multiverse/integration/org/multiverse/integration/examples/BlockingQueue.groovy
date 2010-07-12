package org.multiverse.integration.org.multiverse.integration.examples

import static org.multiverse.integration.MultiverseGroovyLibrary.*
import static org.multiverse.api.StmUtils.*
import org.multiverse.transactional.collections.TransactionalList
import org.multiverse.transactional.collections.TransactionalArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 12 Jul, 2010
 * Time: 9:15:38 PM
 * To change this template use File | Settings | File Templates.
 */

public class BlockingQueue {

  def private TransactionalList buffer
  def maxSize = 2, timeout

  BlockingQueue() {
    buffer = new TransactionalArrayList(maxSize);
  }

  Object dequeue() {
    def ret
    atomic(explicitRetryAllowed: true, trackreads: true) {
      if (buffer.isEmpty()) retry();
      ret = buffer.pop();
    }
    return ret;
  }

  void enqueue(value) {
    atomic(explicitRetryAllowed: true, trackreads: true) {
      if (buffer.size() >= maxSize) retry();
      buffer.add(0, value)
    }
  }
}