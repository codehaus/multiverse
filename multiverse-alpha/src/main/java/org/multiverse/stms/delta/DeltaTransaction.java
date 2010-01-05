package org.multiverse.stms.delta;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.api.exceptions.WriteConflictException;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The goal of the delta stm is to remove the shared clock.
 * <p/>
 * It also introduce the difference between a load for updating and a load for reading. This should reduce the stress
 * made on creating new tranlocal update objects; readonly copies can be used instead.
 * <p/>
 * 2 problems need to be solved: - transaction should not write over transaction that started after it started, but
 * completed before it completed. - transaction should see changes made by transactions that committed before it
 * committed. So a transaction that: - starts and completes before the transaction starts, no problem - starts before
 * and completes after the transaction starts; problem
 *
 * @author Peter Veentjer
 */
public class DeltaTransaction {

    private TransactionStatus status = TransactionStatus.active;

    //private long version = 0;

    private final Map<DeltaAtomicObject, DeltaTranlocal> attached =
            new IdentityHashMap<DeltaAtomicObject, DeltaTranlocal>();

    private long version;

    /**
     * Reads are tracked also for readonly loads, to support the retry functionality.
     *
     * @param atomicObject
     * @return
     */
    public DeltaTranlocal loadReadonly(DeltaAtomicObject atomicObject) {
        switch (status) {
            case active:
                if (atomicObject == null) {
                    return null;
                }

                DeltaTranlocal found = attached.get(atomicObject);
                if (found != null) {
                    return found;
                }

                DeltaTranlocal newest = atomicObject.___load();
                if (newest == null) {
                    throw new LoadUncommittedException();
                }

                if (version == 0) {
                    //lets establish the version for the transaction
                    version = atomicObject.___getHighestTransactionVersion();
                } else if (newest.___version > version) {
                    throw new LoadTooOldVersionException();
                }

                //the tranlocal is attached for read tracking purposes.
                attached.put(atomicObject, newest);
                return newest;
            case aborted:
                throw new DeadTransactionException();
            case committed:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    public DeltaTranlocal loadUpdatable(DeltaAtomicObject atomicObject) {
        switch (status) {
            case active:
                if (atomicObject == null) {
                    return null;
                }

                DeltaTranlocal found = attached.get(atomicObject);
                if (found != null) {
                    //if the value is committed, it should be replaced by an updatable version
                    if (found.___version > 0) {
                        found = found.makeUpdatableClone();
                        attached.put(atomicObject, found);
                    }

                    return found;
                }

                DeltaTranlocal newest = atomicObject.___load();
                DeltaTranlocal created;
                if (newest == null) {
                    created = atomicObject.___createInitialTranlocal();
                } else {
                    //there already has been a commit

                    if (version == 0) {
                        version = atomicObject.___getHighestTransactionVersion();
                    } else if (newest.___version > version) {
                        throw new LoadTooOldVersionException();
                    }

                    created = newest.makeUpdatableClone();
                }
                attached.put(atomicObject, created);
                return created;
            case aborted:
                throw new DeadTransactionException();
            case committed:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    protected void commit() {
        switch (status) {
            case active:
                if (attached.isEmpty()) {
                    status = TransactionStatus.committed;
                    return;
                }

                boolean abort = true;
                try {
                    if (lockAll()) {
                        long highestVersion = noConflicts();
                        if (highestVersion == -1) {
                            throw new WriteConflictException();
                        }

                        abort = false;
                        writeAll(highestVersion + 1);
                    }
                } finally {
                    releaseLocks();
                    if (abort) {
                        status = TransactionStatus.aborted;
                        attached.clear();
                    } else {
                        status = TransactionStatus.committed;
                    }

                }

                break;
            case committed:
                break;
            case aborted:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Call will be made after all locks are acquired.
     *
     * @return
     */
    private long noConflicts() {
        long maxVersion = 0;

        for (Map.Entry<DeltaAtomicObject, DeltaTranlocal> entry : attached.entrySet()) {
            DeltaAtomicObject atomicObject = entry.getKey();
            DeltaTranlocal tranlocal = entry.getValue();

            if (tranlocal.___version > 0) {
                //it is a tranlocal for a read.

                if (tranlocal.___version > maxVersion) {
                    maxVersion = tranlocal.___version;
                }

            } else {
                DeltaTranlocal origin = tranlocal.___origin;

                if (origin == null) {
                    //it is a newly created tranlocal
                    if (maxVersion == 0) {
                        maxVersion = 1;
                    }
                } else {
                    //if the origin differs from the current tranlocal, another transaction made a
                    //commit, so there is a write conflict.
                    if (origin != atomicObject.___loadRaw()) {
                        return -1;
                    }

                    //it is a tranlocal for an update.
                    if (origin.___version > maxVersion) {
                        maxVersion = origin.___version;
                    }
                }
            }
        }

        return maxVersion;
    }

    protected void writeAll(long writeVersion) {
        for (Map.Entry<DeltaAtomicObject, DeltaTranlocal> entry : attached.entrySet()) {
            DeltaAtomicObject atomicObject = entry.getKey();
            DeltaTranlocal tranlocal = entry.getValue();
            //we only need to persist of the version is 0. If it is not 0, it was a tranlocal used for reading
            //purposes.
            if (tranlocal.___version == 0) {
                atomicObject.___store(tranlocal, writeVersion);
            }

            atomicObject.___setHighestTransactionVersion(writeVersion);
        }
    }

    protected boolean lockAll() {
        for (Map.Entry<DeltaAtomicObject, DeltaTranlocal> entry : attached.entrySet()) {
            if (!entry.getKey().___lock(this)) {
                return false;
            }
        }

        return true;
    }

    protected void releaseLocks() {
        for (Map.Entry<DeltaAtomicObject, DeltaTranlocal> entry : attached.entrySet()) {
            if (!entry.getKey().___unlock(this)) {
                return;
            }
        }
    }

    protected void abort() {
        switch (status) {
            case active:
                attached.clear();
                status = TransactionStatus.aborted;
                break;
            case committed:
                throw new DeadTransactionException();
            case aborted:
                //ignore
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public TransactionStatus getStatus() {
        return status;
    }
}
