package org.multiverse.stms.alpha.mixins;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class DefaultTxObjectMixinTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    // ================ storeAndReleaseLock ========================

    @Test
    public void store_initialStore() {
        Transaction lockOwner = mock(Transaction.class);

        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        DummyTranlocal tranlocal = new DummyTranlocal(txObject);

        txObject.___tryLock(lockOwner);
        int writeVersion = 10;
        txObject.___store(tranlocal, writeVersion);

        AlphaTranlocal found = txObject.___load();
        assertSame(tranlocal, found);
        assertEquals(writeVersion, tranlocal.getWriteVersion());
    }

    @Test
    public void store_update() {
        Transaction lockOwner = mock(Transaction.class);

        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        DummyTranlocal tranlocal1 = new DummyTranlocal(txObject);

        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal1, 10);
        txObject.___releaseLock(lockOwner);

        DummyTranlocal tranlocal2 = new DummyTranlocal(txObject);
        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal2, 11);
        txObject.___releaseLock(lockOwner);

        AlphaTranlocal found = txObject.___load();
        assertSame(tranlocal2, found);
        assertEquals(11, tranlocal2.getWriteVersion());
    }


    // ================= load() ==================================


    @Test
    public void loadUncommitted() {
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        AlphaTranlocal tranlocal = txObject.___load();
        assertNull(tranlocal);
    }

    @Test
    public void loadLocked() {
        Transaction lockOwner = mock(Transaction.class);

        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        DummyTranlocal tranlocal = new DummyTranlocal(txObject);

        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal, 10);


        AlphaTranlocal found = txObject.___load();
        assertSame(tranlocal, found);
    }

    @Test
    public void loadCommitted() {
        Transaction lockOwner = mock(Transaction.class);

        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        DummyTranlocal tranlocal = new DummyTranlocal(txObject);

        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal, 10);
        txObject.___releaseLock(lockOwner);

        AlphaTranlocal found = txObject.___load();
        assertSame(tranlocal, found);
    }

    // ================ load(long version) ==========================

    @Test
    public void loadWithVersion_EqualVersion() {
        Transaction lockOwner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        DummyTranlocal tranlocal = new DummyTranlocal(txObject);
        long writeVersion = 10;
        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal, writeVersion);
        txObject.___releaseLock(lockOwner);

        AlphaTranlocal result = txObject.___load(writeVersion);
        assertSame(tranlocal, result);
    }

    @Test
    public void loadWithVersion_WithNewVersion() {
        Transaction lockOwner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        DummyTranlocal tranlocal = new DummyTranlocal(txObject);
        long writeVersion = 10;
        txObject.___tryLock(lockOwner);
        txObject.___store(tranlocal, writeVersion);
        txObject.___releaseLock(lockOwner);

        AlphaTranlocal result = txObject.___load(writeVersion + 1);
        assertSame(tranlocal, result);
    }

    @Test
    public void loadWithVersion_uncommittedData() {
        DummyFastTransactionalObjectMixin object = new DummyFastTransactionalObjectMixin();

        AlphaTranlocal result = object.___load(1);
        assertNull(result);
    }

    @Test
    public void loadWithVersion_tooNewVersion() {
        IntRef txObject = new IntRef(0);

        long version = stm.getVersion();
        txObject.inc();

        try {
            txObject.___load(version);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }
    }

    @Test
    public void loadWithVersion_whileLockedFailsIfCommittedVersionEqualToReadVersion() {
        IntRef intValue = new IntRef(0);

        long readVersion = stm.getVersion();

        Transaction owner = mock(Transaction.class);
        IntRefTranlocal old = (IntRefTranlocal) intValue.___load();
        intValue.___tryLock(owner);

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        intValue.___releaseLock(owner);
        assertSame(old, intValue.___load());
    }

    @Test
    public void loadWithVersion_whileLockedFailsIfTheCommittedVersionIsOlderThanReadVersion() {
        IntRef intValue = new IntRef(0);

        Transaction owner = mock(Transaction.class);
        intValue.___tryLock(owner);

        long readVersion = stm.getVersion() + 1;

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }
    }

    @Test
    public void loadWithVersion_WhileLockedFailsIfTheCommittedVersionIsNewerThanReadVersion() {
        IntRef intValue = new IntRef(0);
        long readVersion = stm.getVersion();
        intValue.inc();

        Transaction owner = mock(Transaction.class);
        intValue.___tryLock(owner);

        try {
            intValue.___load(readVersion);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }
    }


    // ================ tryLock ==========================

    @Test
    public void tryLock_succeedsIfLockIsFree() {
        Transaction lockOwner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        boolean result = txObject.___tryLock(lockOwner);
        assertTrue(result);
        assertSame(lockOwner, txObject.___getLockOwner());
    }

    @Test
    public void tryLock_reentrantLockFails() {
        Transaction owner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        txObject.___tryLock(owner);

        boolean result = txObject.___tryLock(owner);
        assertFalse(result);
        assertSame(owner, txObject.___getLockOwner());
    }

    @Test
    public void tryLock_failsIfLockOwnedByOthers() {
        Transaction oldOwner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        txObject.___tryLock(oldOwner);

        Transaction newOwner = mock(Transaction.class);
        boolean result = txObject.___tryLock(newOwner);
        assertFalse(result);
        assertSame(oldOwner, txObject.___getLockOwner());
    }

    // ======================= release lock ====================

    @Test
    public void releaseLock_whenLockIsOwnedLockIsFreed() {
        Transaction owner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        txObject.___tryLock(owner);

        txObject.___releaseLock(owner);
        assertNull(txObject.___getLockOwner());
    }

    @Test
    public void releaseLock_whenLockIsFreeCallIsIgnored() {
        Transaction owner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();

        txObject.___releaseLock(owner);
        assertNull(txObject.___getLockOwner());
    }

    @Test
    public void releaseLock_whenLockIsNotOwnedCallIsIgnored() {
        Transaction otherOwner = mock(Transaction.class);
        Transaction thisOwner = mock(Transaction.class);
        DummyFastTransactionalObjectMixin txObject = new DummyFastTransactionalObjectMixin();
        txObject.___tryLock(otherOwner);

        txObject.___releaseLock(thisOwner);
        assertSame(otherOwner, txObject.___getLockOwner());
    }

    // ==========================================

    static class DummyFastTransactionalObjectMixin extends DefaultTxObjectMixin {

        @Override
        public AlphaTranlocal ___openUnconstructed() {
            throw new RuntimeException();
        }
    }

    // TODO: merge with org.multiverse.stms.alpha.DummyTranlocal?
    static class DummyTranlocal extends AlphaTranlocal {

        private AlphaTransactionalObject transactionalObject;

        DummyTranlocal(AlphaTransactionalObject transactionalObject) {
            this.transactionalObject = transactionalObject;
        }

        @Override
        public void prepareForCommit(long writeVersion) {
            ___writeVersion = writeVersion;
        }

        @Override
        public AlphaTranlocal openForWrite() {
            throw new RuntimeException();
        }

        @Override
        public AlphaTranlocal getOrigin() {
            throw new RuntimeException();
        }

        @Override
        public AlphaTranlocalSnapshot takeSnapshot() {
            throw new RuntimeException();
        }

        @Override
        public boolean isDirty() {
            throw new RuntimeException();
        }

        @Override
        public AlphaTransactionalObject getTransactionalObject() {
            return transactionalObject;
        }
    }
}
