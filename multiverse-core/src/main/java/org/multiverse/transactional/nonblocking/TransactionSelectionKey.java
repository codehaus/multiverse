package org.multiverse.transactional.nonblocking;

import org.multiverse.api.Transaction;

public interface TransactionSelectionKey {

    Transaction getTransaction();
}
