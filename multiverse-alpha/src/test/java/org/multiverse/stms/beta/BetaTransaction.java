package org.multiverse.stms.beta;

import org.multiverse.api.Transaction;

public interface BetaTransaction extends Transaction {

    BetaTranlocal openForRead(BetaObject object);

    BetaTranlocal openForWrite(BetaObject object);
}
