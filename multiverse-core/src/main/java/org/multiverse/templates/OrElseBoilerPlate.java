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
        failIfTheCallablesAreNull(either, orelse);
        try {
            return either.call(tx);
        } catch (Retry e) {
            return orelse.call(tx);
        } 
    }

    private void failIfTheCallablesAreNull(EitherCallable either, OrElseCallable orelse) {
        if(either == null){
            throw new NullPointerException("Either Callable cannot be Null");
        }
        if(orelse == null){
            throw new NullPointerException("OrElse Callable cannot be Null");
        }        
    }
}
