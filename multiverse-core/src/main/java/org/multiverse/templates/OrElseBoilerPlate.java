package org.multiverse.templates;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.Retry;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public final class OrElseBoilerPlate {
    private final Transaction tx;

    public OrElseBoilerPlate(){
        this(getThreadLocalTransaction());
    }

    public OrElseBoilerPlate(final Transaction tx) {
        this.tx = tx;
    }
    
    public final <E> E execute(final EitherCallable<E> either, final OrElseCallable<E> orelse) throws Exception {
        try {
            return either.call(tx);
        } catch (Retry e) {
            return orelse.call(tx);
        }
    }    
}
