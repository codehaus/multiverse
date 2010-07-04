package org.multiverse.integration

import static org.multiverse.api.StmUtils.*

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

}