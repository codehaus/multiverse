package org.multiverse.stms.alpha.mixins;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.DummyTransaction;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.stms.alpha.*;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;

public class FastAtomicObjectMixinTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    // ================ storeAndReleaseLock ========================

    @Test
    public void storeAndReleaseLock() {
        testIncomplete();
    }

    // ================= validate ==================================

    @Test
    public void validate() {
        testIncomplete();
    }

    // ================ load ==========================

    @Test
    public void loadEqualVersion() {
        Transaction lockOwner = new DummyTransaction();
        DummyFastAtomicObjectMixin atomicObject = new DummyFastAtomicObjectMixin();

        DummyTranlocal tranlocal = new DummyTranlocal(atomicObject);
        long writeVersion = 10;
        atomicObject.___tryLock(lockOwner);
        atomicObject.___storeAndReleaseLock(tranlocal, writeVersion);
        atomicObject.___releaseLock(lockOwner);

        AlphaTranlocal result = atomicObject.___load(writeVersion);
        assertSame(tranlocal, result);
    }

    @Test
    public void loadWithNewVersion() {
        Transaction lockOwner = new DummyTransaction();
        DummyFastAtomicObjectMixin atomicObject = new DummyFastAtomicObjectMixin();

        DummyTranlocal tranlocal = new DummyTranlocal(atomicObject);
        long writeVersion = 10;
        atomicObject.___tryLock(lockOwner);
        atomicObject.___storeAndReleaseLock(tranlocal, writeVersion);
        atomicObject.___releaseLock(lockOwner);

        AlphaTranlocal result = atomicObject.___load(writeVersion + 1);
        assertSame(tranlocal, result);
    }

    @Test
    public void loadUncommittedData() {
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();

        AlphaTranlocal result = object.___load(1);
        assertNull(result);
    }

    @Test
    public void loadTooNewVersion() {
        IntRef intValue = new IntRef(0);

        long version = stm.getTime();
        intValue.inc();

        try {
            intValue.___load(version);
            fail();
        } catch (LoadTooOldVersionException ex) {
        }
    }

    @Test
    public void loadWhileLockedSucceedsIfCommittedVersionIsEqualToReadVersion() {
        IntRef intValue = new IntRef(0);

        long readVersion = stm.getTime();

        Transaction owner = new DummyTransaction();
        IntRefTranlocal old = (IntRefTranlocal) intValue.___load();
        intValue.___tryLock(owner);

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LoadLockedException expected) {
        }

        intValue.___releaseLock(owner);
        assertSame(old, intValue.___load());
    }

    @Test
    public void loadWhileLockedFailsIfTheCommittedVersionIsOlderThanReadVersion() {
        IntRef intValue = new IntRef(0);

        Transaction owner = new DummyTransaction();
        intValue.___tryLock(owner);

        long readVersion = stm.getTime() + 1;

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LoadLockedException ex) {
        }
    }

    @Test
    public void loadWhileLockedFailsIfTheCommittedVersionIsNewerThanReadVersion() {
        IntRef intValue = new IntRef(0);
        long readVersion = stm.getTime();
        intValue.inc();

        Transaction owner = new DummyTransaction();
        intValue.___tryLock(owner);

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LoadLockedException ex) {
        }
    }


    // ================ acquireLock ==========================

    @Test
    public void acquireFreeLock() {
        Transaction lockOwner = new DummyTransaction();
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();

        boolean result = object.___tryLock(lockOwner);
        assertTrue(result);
        assertSame(lockOwner, object.___getLockOwner());
    }

    @Test
    public void acquireAlreadyOwnedLock() {
        testIncomplete();
    }

    @Test
    public void acquireLockedOwnedByOther() {
        Transaction oldOwner = new DummyTransaction();
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();

        object.___tryLock(oldOwner);

        Transaction newOwner = new DummyTransaction();
        boolean result = object.___tryLock(newOwner);
        assertFalse(result);
        assertSame(oldOwner, object.___getLockOwner());
    }

    // ======================= release lock ====================

    @Test
    public void releaseOwnedLock() {
        Transaction owner = new DummyTransaction();
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();
        object.___tryLock(owner);

        object.___releaseLock(owner);
        assertNull(object.___getLockOwner());
    }

    @Test
    public void releaseFreeLock() {
        Transaction owner = new DummyTransaction();
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();

        object.___releaseLock(owner);
        assertNull(object.___getLockOwner());
    }

    @Test
    public void releaseNotOwnedLock() {
        Transaction otherOwner = new DummyTransaction();
        Transaction thisOwner = new DummyTransaction();
        DummyFastAtomicObjectMixin object = new DummyFastAtomicObjectMixin();
        object.___tryLock(otherOwner);

        object.___releaseLock(thisOwner);
        assertSame(otherOwner, object.___getLockOwner());
    }

    // ==========================================

    static class DummyFastAtomicObjectMixin extends FastAtomicObjectMixin {

        @Override
        public AlphaTranlocal ___loadUpdatable(long readVersion) {
            throw new RuntimeException();
        }
    }

    // TODO: merge with org.multiverse.stms.alpha.DummyTranlocal?
    static class DummyTranlocal extends AlphaTranlocal {

        private AlphaAtomicObject atomicObject;

        DummyTranlocal(AlphaAtomicObject atomicObject) {
            this.atomicObject = atomicObject;
        }

        @Override
        public void prepareForCommit(long writeVersion) {
            ___writeVersion = writeVersion;
        }

        @Override
        public AlphaTranlocalSnapshot takeSnapshot() {
            throw new RuntimeException();
        }

        @Override
        public DirtinessStatus getDirtinessStatus() {
            throw new RuntimeException();
        }

        @Override
        public AlphaAtomicObject getAtomicObject() {
            return atomicObject;
        }
    }
}
