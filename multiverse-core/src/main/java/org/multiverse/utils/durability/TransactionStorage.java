package org.multiverse.utils.durability;

import org.multiverse.api.Transaction;

public interface TransactionStorage {

    void writeBehind(Transaction tx); 

    void writeThrough(Transaction tx);
}
