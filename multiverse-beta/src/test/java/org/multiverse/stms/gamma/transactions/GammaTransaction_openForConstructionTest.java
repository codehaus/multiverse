package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;

public abstract class GammaTransaction_openForConstructionTest<T extends GammaTransaction>{
    protected GammaStm stm;

    @Before
    public void setUp(){
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    @Test
    @Ignore
    public void whenReadonlyTransaction(){

    }

    @Test
    @Ignore
    public void whenSuccess(){

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForConstruction(){

    }

    @Test
    @Ignore
    public void whenOpenedForRead(){

    }

    @Test
    @Ignore
    public void whenOpenedForWrite(){

    }

    @Test
    @Ignore
    public void whenOpenedForCommute(){

    }

    @Test
    @Ignore
    public void whenStmMismatch(){

    }

    @Test
    @Ignore
    public void whenTransactionAlreadyPrepared(){

    }

    @Test
    @Ignore
    public void whenTransactionAlreadyAborted(){

    }

    @Test
    @Ignore
    public void whenTransactionAlreadyCommitted(){

    }
}
