package org.multiverse.templates;

import org.multiverse.api.Transaction;


public interface EitherCallable<V>{
        V call(Transaction tx) throws Exception;
    }