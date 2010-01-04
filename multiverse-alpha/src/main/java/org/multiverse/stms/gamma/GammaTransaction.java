package org.multiverse.stms.gamma;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.TodoException;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Idea behind the gamma tranlocal is that objects only are used with certain other objects,
 * can have their own counter.
 */
public class GammaTransaction {

    private TransactionStatus status = TransactionStatus.active;

    private final Map<GammaTransactionalObject, GammaTranlocal> attached =
            new IdentityHashMap<GammaTransactionalObject, GammaTranlocal>();

    private VectorClock clock;

    public GammaTranlocal load(GammaTransactionalObject transactionalObject) {
        switch (status) {
            case active:
                if (transactionalObject == null) {
                    return null;
                }

                GammaTranlocal found = attached.get(transactionalObject);
                if (found != null) {
                    return found;
                }

                found = transactionalObject.___load();
                if (found == null) {
                    found = transactionalObject.___createInitialTranlocal();
                } else {
                    found = found.makeUpdatableClone();
                }

                throw new TodoException();
            case committed:
                throw new DeadTransactionException();
            case aborted:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }

    public void abort() {
        switch (status) {
            case active:
                status = TransactionStatus.aborted;
                attached.clear();
                break;
            case committed:
                throw new DeadTransactionException();
            case aborted:
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void commit() {
        switch (status) {
            case active:
                if (attached.isEmpty()) {
                    status = TransactionStatus.committed;
                    return;
                }

                throw new TodoException();
            case committed:
                break;
            case aborted:
                throw new DeadTransactionException();
            default:
                throw new IllegalStateException();
        }
    }
}
