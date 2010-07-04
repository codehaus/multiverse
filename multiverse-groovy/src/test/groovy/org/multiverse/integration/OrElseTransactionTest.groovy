package org.multiverse.integration

import static org.multiverse.api.StmUtils.*
import org.multiverse.transactional.refs.LongRef
import org.multiverse.api.exceptions.ReadonlyException

/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 3 Jul, 2010
 * Time: 11:17:01 PM
 * To change this template use File | Settings | File Templates.
 */

public class OrElseTransactionTest extends GroovyTestCase {

  void testShouldExecuteTheEitherSectionWhenThereAreNoIssuesInThatSection() {

    def executedSection = ""

    new OrElseTransaction().identity {
      either {
        executedSection = "either"
      }
      orelse {
        executedSection = "orelse"
      }
      execute()
    }

    assertEquals("either", executedSection)

  }

  void testShouldExecuteOrElseSectionWhenEitherSectionFails() {
    def executedSection = ""

    new OrElseTransaction().identity {
      either {
        executedSection = "either"
        retry()
      }
      orelse {
        executedSection = "orelse"
      }
      execute()
    }

    assertEquals("orelse", executedSection)
  }

  void testExceptionInEitherSectionShouldBeBubbledUpToCaller() {
    shouldFail(RuntimeException) {
      new OrElseTransaction().identity {
        either {
          throw new RuntimeException();
        }
        execute()
      }
    }
  }

  void testExceptionInOrElseSectionShouldBeBubbledUpToCaller() {
    shouldFail(RuntimeException) {
      new OrElseTransaction().identity {
        either {
          retry()
        }
        orelse {
          throw new RuntimeException();
        }
        execute()
      }
    }
  }

  void testShouldDisallowModificationsForReadonlyTransactions() {

    LongRef number = new LongRef();

    shouldFail(ReadonlyException) {
      new OrElseTransaction().identity {
        either {
          number.inc()
        }
        config readonly: true
        execute()
      }
    }

    assertEquals(0, number.get())

  }

}