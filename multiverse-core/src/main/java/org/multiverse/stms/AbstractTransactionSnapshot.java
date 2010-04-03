package org.multiverse.stms;

import org.multiverse.api.lifecycle.TransactionLifecycleListener;

public abstract class AbstractTransactionSnapshot {

    protected TransactionLifecycleListener tasks;
    protected AbstractTransactionSnapshot parent;

    public abstract void restore();
}
