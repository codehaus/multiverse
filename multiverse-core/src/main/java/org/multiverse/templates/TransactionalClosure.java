package org.multiverse.templates;

import org.multiverse.api.Transaction;

import java.util.concurrent.Callable;

/**
 *TransactionalTemplate to be used with Callable
 * <p/>
 * example:
 * <pre>
 * new TransactionalClosure().execute(new Callable<Integer> {
 *         Integer call(){
 *            return queue.pop();
 *         }
 * }).execute();
 * </pre>
 * <p/>
 *  
 * @author Sai Venkat
 */

public class TransactionalClosure<E> extends TransactionTemplate<E> {
    private Callable<E> callable;

    @Override
    public E execute(Transaction tx) throws Exception {
        return this.callable.call();
    }
    public final E execute(Callable<E> callable) {
        this.callable = callable;
        return execute();
    }
}
