package org.multiverse.templates;

import org.multiverse.api.Transaction;

public interface OrElseCallable<V>{
        V call(Transaction tx) throws Exception;
    }
