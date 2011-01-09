package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.RetryNotAllowedException;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;

public abstract class GammaTransaction_retryTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp(){
        stm = new GammaStm();
    }

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    protected abstract T newTransaction();

    @Test
    @Ignore
    public void whenContainsRead(){

    }

    @Test
    @Ignore
    public void whenContainsWrite(){

    }

    @Test
    @Ignore
    public void whenContainsConstructed(){

    }

    @Test
    @Ignore
    public void whenContainsCommute(){

    }

    @Test
    @Ignore
    public void whenUnused(){

    }

    @Test
    public void whenNoRetryAllowed(){
        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.isBlockingAllowed = false;

        T tx = newTransaction(config);
        try{
            tx.retry();
            fail();
        }catch(RetryNotAllowedException expected){
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyAborted(){
        T tx = newTransaction();
        tx.abort();

        try{
            tx.retry();
            fail();
        }catch(DeadTransactionException expected){
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted(){
        T tx = newTransaction();
        tx.commit();

        try{
            tx.retry();
            fail();
        }catch(DeadTransactionException expected){
        }

        assertIsCommitted(tx);
    }
}
