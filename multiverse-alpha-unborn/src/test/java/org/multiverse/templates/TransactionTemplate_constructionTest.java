package org.multiverse.templates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactoryBuilder;

import static org.junit.Assert.assertFalse;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionTemplate_constructionTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        TransactionTemplate template = new TransactionTemplate(){
            @Override
            public Object execute(Transaction tx) throws Exception {
                return null;  //todo
            }
        };

        TransactionFactoryBuilder builder = template.getTransactionFactory().getBuilder();
        assertFalse(builder.isReadonly());
        assertFalse(builder.isReadTrackingEnabled());                
    }
}
