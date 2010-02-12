package org.multiverse.stms.beta;

public interface BetaTranlocal {

    BetaTranlocal ___openForWrite();

    boolean ___isCommitted();

    boolean ___isDirty();

    void ___store(long writeVersion);

    long ___getWriteVersion();
}
