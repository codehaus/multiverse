package org.multiverse.templates;

import org.multiverse.api.Transaction;

import java.util.concurrent.Callable;


public interface EitherCallable<V> {
        V call(Transaction tx) throws Exception;
}