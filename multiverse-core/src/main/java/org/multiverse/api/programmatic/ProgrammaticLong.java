package org.multiverse.api.programmatic;

import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public interface ProgrammaticLong {

    long get(Transaction tx);

    void set(Transaction tx, long newValue);

    long get();

    long getAtomic();

    void set(long newValue);

    long inc(Transaction tx, long amount);

    void commutingInc(Transaction tx, long amount);

    void commutingInc(long amount);
}
