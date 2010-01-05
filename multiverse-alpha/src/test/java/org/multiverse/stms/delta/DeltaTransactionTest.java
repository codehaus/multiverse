package org.multiverse.stms.delta;

import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class DeltaTransactionTest {

    /*
    @Test
    public void loadKeepsFailing(){
        DeltaAtomicObject atomicObject1 = new DeltaAtomicObject();
        DeltaAtomicObject atomicObject2 = new DeltaAtomicObject();
        atomicObject2.inc();

        DeltaTransaction t = new DeltaTransaction();
        t.loadUpdatable(atomicObject1);
        try{
            t.loadUpdatable(atomicObject2);
            fail();
        }catch(LoadTooOldVersionException ex){
        }
    }

    @Test
    public void loadInconsistencyIsDetected() {
        DeltaTransaction t1 = new DeltaTransaction();
        DeltaAtomicObject atomicObject1 = new DeltaAtomicObject(t1);
        DeltaAtomicObject atomicObject2 = new DeltaAtomicObject(t1);
        t1.commit();

        DeltaTransaction t2 = new DeltaTransaction();
        atomicObject1.inc(t2);

        atomicObject2.inc();

        try {
            atomicObject2.inc(t2);
            fail();
        } catch (LoadTooOldVersionException ex) {
        }
    }

    @Test
    public void commitWithoutChanges() {
        DeltaAtomicObject atomicObject = new DeltaAtomicObject();
        DeltaTranlocal tranlocal = atomicObject.___loadRaw();
        long version = atomicObject.___getHighestTransactionVersion();

        DeltaTransaction t = new DeltaTransaction();
        t.loadReadonly(atomicObject);
        t.commit();

        assertSame(tranlocal, atomicObject.___loadRaw());
        assertEquals(version + 1, atomicObject.___getHighestTransactionVersion());
    }

    @Test
    public void multipleAtomicObjects() {
        DeltaAtomicObject atomicObject1 = new DeltaAtomicObject();
        DeltaAtomicObject atomicObject2 = new DeltaAtomicObject();

        DeltaTransaction t = new DeltaTransaction();
        atomicObject1.inc(t);
        atomicObject2.inc(t);
        t.commit();

        assertEquals(TransactionStatus.committed, t.getStatus());

        DeltaTranlocal tranlocal1 = atomicObject1.___loadRaw();
        assertNotNull(tranlocal1);
        assertNull(tranlocal1.___origin);
        assertEquals(1, tranlocal1.value);

        DeltaTranlocal tranlocal2 = atomicObject2.___loadRaw();
        assertNotNull(tranlocal2);
        assertNull(tranlocal2.___origin);
        assertEquals(1, tranlocal2.value);
    }

    @Test
    public void loadUpdatableFromNonCommittedObject() {
        DeltaTransaction t = new DeltaTransaction();
        DeltaAtomicObject atomicObject = new DeltaAtomicObject(t);
        DeltaTranlocal tranlocal = t.loadUpdatable(atomicObject);
        assertNotNull(tranlocal);
        assertNull(tranlocal.___origin);
        assertEquals(0, tranlocal.___version);
    }

    @Test
    public void commitInitial() {
        DeltaTransaction t = new DeltaTransaction();
        DeltaAtomicObject deltaAtomicObject = new DeltaAtomicObject(t);
        t.commit();

        assertEquals(TransactionStatus.committed, t.getStatus());
        assertFalse(deltaAtomicObject.___isLocked());
        DeltaTranlocal tranlocal = deltaAtomicObject.___load();
        assertNotNull(tranlocal);
        assertNull(tranlocal.___origin);
        assertEquals(1, tranlocal.___version);
        //assertEquals(1, deltaAtomicObject.___getNewestSpottedVersion());
    }

    @Test
    public void commitDeltaAtomicObject() {
        DeltaAtomicObject atomicObject = new DeltaAtomicObject();

        assertFalse(atomicObject.___isLocked());
        DeltaTranlocal tranlocal = atomicObject.___load();
        assertNotNull(tranlocal);
        assertNull(tranlocal.___origin);
        assertEquals(1, tranlocal.___version);
        //assertEquals(1, atomicObject.___getNewestSpottedVersion());
    }

    @Test
    public void writeConflict() {
        DeltaAtomicObject atomicObject = new DeltaAtomicObject();

        DeltaTransaction transaction = new DeltaTransaction();
        DeltaTranlocal tranlocal = transaction.loadUpdatable(atomicObject);
        atomicObject.inc();
        DeltaTranlocal committed = atomicObject.___loadRaw();
        atomicObject.inc(transaction);

        try {
            transaction.commit();
            fail();
        } catch (WriteConflictException ex) {

        }

        assertEquals(TransactionStatus.aborted, transaction.getStatus());
        assertSame(committed, atomicObject.___loadRaw());
        assertEquals(committed.___version, atomicObject.___getHighestTransactionVersion());
    }

    @Test
    public void noWriteConflictIsCausedWhenVersionIsDelayed() {
        DeltaAtomicObject atomicObject = new DeltaAtomicObject();
        long version = atomicObject.___getHighestTransactionVersion();

        DeltaTransaction transaction = new DeltaTransaction();
        atomicObject.inc();
        atomicObject.inc(transaction);

        transaction.commit();

        assertEquals(TransactionStatus.committed, transaction.getStatus());
        assertEquals(version + 2, atomicObject.___getHighestTransactionVersion());
        assertEquals(2, atomicObject.get());
    }

    @Test
    public void commitUpdate() {
        DeltaAtomicObject atomicObject = new DeltaAtomicObject();

        DeltaTransaction transaction = new DeltaTransaction();
        atomicObject.inc(transaction);
        transaction.commit();

        assertEquals(1, atomicObject.get());
    }

    @Test
    public void commitOnEmptyTransactionSucceeds() {
        DeltaTransaction t = new DeltaTransaction();
        t.commit();

        assertEquals(TransactionStatus.committed, t.getStatus());
    }

    @Test
    public void commitOnAbortedTransactionFails() {
        DeltaTransaction t = new DeltaTransaction();
        t.abort();

        try {
            t.commit();
            fail();
        } catch (DeadTransactionException ex) {

        }
        assertEquals(TransactionStatus.aborted, t.getStatus());
    }

    @Test
    public void commitOnCommittedTransactionIsIgnored() {
        DeltaTransaction t = new DeltaTransaction();
        t.commit();

        t.commit();
        assertEquals(TransactionStatus.committed, t.getStatus());
    } */

    @Test
    public void test() {
    }

}
