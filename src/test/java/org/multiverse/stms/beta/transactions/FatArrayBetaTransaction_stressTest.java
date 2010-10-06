package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class FatArrayBetaTransaction_stressTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckEnabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(true);
    }

    @Test
    public void integrationTest_whenMultipleUpdatesAndDirtyCheckDisabled() {
        integrationTest_whenMultipleUpdatesAndDirtyCheck(false);
    }

    public void integrationTest_whenMultipleUpdatesAndDirtyCheck(final boolean dirtyCheck) {
        BetaLongRef[] refs = new BetaLongRef[30];
        long created = 0;

        //create the references
        for (int k = 0; k < refs.length; k++) {
            refs[k] = newLongRef(stm);
        }

        //execute all transactions
        Random random = new Random();
        int transactionCount = 100000;
        for (int transaction = 0; transaction < transactionCount; transaction++) {
            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length)
                    .setDirtyCheckEnabled(dirtyCheck);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            for (int k = 0; k < refs.length; k++) {
                if (random.nextInt(3) == 1) {
                    tx.openForWrite(refs[k], LOCKMODE_NONE).value++;
                    created++;
                } else {
                    tx.openForWrite(refs[k], LOCKMODE_NONE);
                }
            }
            tx.commit();
            tx.softReset();
        }

        long sum = 0;
        for (int k = 0; k < refs.length; k++) {
            sum += refs[k].atomicGet();
        }

        assertEquals(created, sum);
    }

}
