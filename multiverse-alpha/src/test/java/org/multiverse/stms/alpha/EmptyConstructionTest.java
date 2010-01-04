package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

import org.multiverse.api.Transactions;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

/**
 * A regression test that makes sure that the alpha stm is able to deal with constructors that
 * don't do a openForWrite on the transaction. If this isn't done, nothing is stored. Although this gives
 * no problems for the other update transactions, it is a problem for readonly transactions because they
 * will suffer from a LoadUncommittedException.
 *
 * @author Peter Veentjer. 
 */
public class EmptyConstructionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm)getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    @Test(expected = LoadUncommittedException.class)
    public void problematicConstructionFollowedByReadonlyTransactionCausesProblem(){
        ProblematicObject ref = new ProblematicObject();

        AlphaTransaction tx = (AlphaTransaction)Transactions.startReadonlyTransaction(stm);
        tx.openForRead((AlphaTransactionalObject)((Object)ref));
    }

    @Test
    public void problematicConstructionFollowedByUpdateTransactionSucceeds(){
        ProblematicObject ref = new ProblematicObject();

        AlphaTransaction tx = startTrackingUpdateTransaction(stm);
        AlphaTranlocal tranlocal = tx.openForWrite((AlphaTransactionalObject)((Object)ref));

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.getTransactionalObject());
        assertNull(tranlocal.getOrigin());
        assertTrue(tranlocal.isUncommitted());
    }

    @TransactionalObject
    static class ProblematicObject {
        Object value;

        ProblematicObject(){
        }

        ProblematicObject(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
