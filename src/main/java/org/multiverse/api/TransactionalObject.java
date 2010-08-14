package org.multiverse.api;

public interface TransactionalObject {

    LockStatus getLockStatus(Transaction tx);
}
