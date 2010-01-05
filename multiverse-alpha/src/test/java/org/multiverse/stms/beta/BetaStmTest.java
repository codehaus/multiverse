package org.multiverse.stms.beta;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class BetaStmTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Ignore
    @Test
    public void testDelayed(){
        DelayedBetaAtomicObject atomicObject = new DelayedBetaAtomicObject(stm);

        atomicObject.inc(stm);

        assertEquals(1, atomicObject.getValue(stm));
    }

    /*
    @Ignore
    @Test
    public void testDelayedDoesNotConflict(){
        DelayedBetaAtomicObject atomicObject = new DelayedBetaAtomicObject(stm);

        BetaTransaction t = stm.startUpdateTransaction((String)null);
        DelayedBetaTranlocal tranlocal = (DelayedBetaTranlocal)t.load(atomicObject);

        atomicObject.inc(stm);

        atomicObject.inc(t);
        t.commit();

        assertEquals(2, atomicObject.getValue(stm));
    }

    @Ignore
    @Test
    public void test() {
        long version = stm.getTime();

        BetaTransaction t = stm.startUpdateTransaction((String)null);
        NormalBetaAtomicObject atomicObject = new NormalBetaAtomicObject(t);
        BetaTranlocal tranlocal = t.load(atomicObject);
        t.commit();

        assertSame(tranlocal, atomicObject.load());
        assertEquals(version + 1, stm.getTime());
        assertEquals(stm.getTime(), tranlocal.___version);
    }

    @Ignore
    @Test
    public void test2() {
        NormalBetaAtomicObject atomicObject1 = new NormalBetaAtomicObject(stm);
        NormalBetaAtomicObject atomicObject2 = new NormalBetaAtomicObject(stm);

        long version = stm.getTime();

        BetaTransaction t = stm.startUpdateTransaction((String)null);
        NormalBetaTranlocal tranlocal1 = (NormalBetaTranlocal)t.load(atomicObject1);
        NormalBetaTranlocal tranlocal2 = (NormalBetaTranlocal)t.load(atomicObject2);
        tranlocal1.value++;
        tranlocal2.value++;
        t.commit();

        assertSame(tranlocal1, atomicObject1.load());
        assertEquals(1, tranlocal1.value);
        assertEquals(stm.getTime(), tranlocal1.___version);
        assertSame(tranlocal2, atomicObject2.load());
        assertEquals(1, tranlocal2.value);
        assertEquals(stm.getTime(), tranlocal2.___version);

        assertEquals(version + 1, stm.getTime());
    }

    @Ignore
    @Test
    public void nonRepairableWriteConflict() {
        NormalBetaAtomicObject atomicObject = new NormalBetaAtomicObject(stm);

        BetaTransaction transaction = stm.startUpdateTransaction((String)null);
        NormalBetaTranlocal tranlocal = (NormalBetaTranlocal)transaction.load(atomicObject);

        atomicObject.inc(stm);

        tranlocal.value++;
        try {
            transaction.commit();
            fail();
        } catch (WriteConflictException expected) {
        }

    }

    @Ignore
    @Test
    public void repairableWriteConflict() {
        NormalBetaAtomicObject atomicObject = new NormalBetaAtomicObject(stm);

        BetaTransaction transaction = stm.startUpdateTransaction((String)null);
        NormalBetaTranlocal tranlocal = (NormalBetaTranlocal)transaction.load(atomicObject);

        //let another transaction do an update.
        atomicObject.inc(stm);

        //now lets update that causes the conflict.
        atomicObject.inc(transaction);

        //because the
        transaction.commit();

        //assertEquals(2, atomicObject.load().value);
    }
      */
}
