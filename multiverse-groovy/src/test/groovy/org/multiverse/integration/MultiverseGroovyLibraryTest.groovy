package org.multiverse.integration
/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 27 Jun, 2010
 * Time: 9:48:34 PM
 * To change this template use File | Settings | File Templates.
 */

import static org.multiverse.integration.MultiverseGroovyLibrary.*
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.transactional.refs.LongRef
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict

import org.multiverse.api.exceptions.TooManyRetriesException
import static org.multiverse.api.StmUtils.*
import org.multiverse.api.exceptions.RetryTimeoutException

public class MultiverseGroovyLibraryTest extends GroovyTestCase {


  void testThrowExceptionsThatHappenWithinTheClosure() {
    shouldFail(RuntimeException) {
      atomic() {
        throw new RuntimeException();
      }
    }
  }

  void testShouldRetryTransactionForTheConfiguredNumberOfTimes() {
    def actualNumberOfRetries = 0
    shouldFail(TooManyRetriesException) {
      atomic(maxRetries: 3) {
        actualNumberOfRetries++;
        throw new OldVersionNotFoundReadConflict();
      }
    }

    assertEquals(4, actualNumberOfRetries)
  }

  void testShouldDisallowModificationsForReadonlyTransactions() {

    final LongRef number = new LongRef(0);

    shouldFail(ReadonlyException) {
      atomic(readonly: true) {
        number.inc();
      }
    }

    assertEquals(0, number.get());
  }

  void testShouldAllowManualRetryWhenExplicitRetryAndReadTrackingAreEnabled() {

    def actualNumberOfRetries = 0;
    final LongRef valueForReadTracking = new LongRef(0);

    shouldFail(RetryTimeoutException) {
      atomic(trackreads: true, explicitRetryAllowed: true, timeoutInNanoSeconds: 10) {
        actualNumberOfRetries++;
        valueForReadTracking.inc()
        retry()
      }
    }

    assertEquals(1, actualNumberOfRetries)
    assertEquals(0, valueForReadTracking.get())

  }

  void testShouldExecuteElseIfOrFailed() {

    def executedSection = ""

    orElseTransaction(
            {
              executedSection = "either"
              retry()
            },
            {
              executedSection = "or"
            }
    )
    
    assertEquals("or", executedSection)

  }

}

