package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Test to make sure that the equals/hash of a transactional object is not used inside transactions.
 *
 * @author Peter Veentjer
 */
public class EqualsAndHashNotUsedTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        Ref ref = new Ref(10);
        ref.getValue();
        ref.setValue(100);
    }

    @TransactionalObject
    class Ref {
        private int value;

        public Ref(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean equals(Object item) {
            throw new RuntimeException();
        }

        public int hashCode() {
            throw new RuntimeException();
        }
    }
}
