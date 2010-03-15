package org.multiverse.templates;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.Retry;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A template for the 'orelse' functionality provided by the STM. Orelse has to do with blocking and not with clashing
 * transactions. With the or else functionality you are able to block on multiple structures, for example (pseudo
 * code):
 * <pre>
 * def stack1: new TransactionalStack();
 * def stack2: new TransactionalStack();
 * <p/>
 * atomic{
 *   return stack1.pop();
 * }orelse{
 *   return stack2.pop();
 * }
 * </pre>
 * <p/>
 * The possible outcomes are: <ol> <li>an item from stack 1 is popped and returned</li> <li>an item from stack 2 is
 * popped and returned</li> <li>the transaction is going to block on stack1 and stack2 and is woken up once an item is
 * placed on either of them. Once the transaction is woken up, it reexecutes the complete transaction.</li> </ol> This
 * functionality is very hard to realize with classic lock based concurrency, but no problem with STM's.
 * <p/>
 * And OrElse templates are allowed to be nested!
 * <p/>
 * A template for the 'orelse' functionality. Example:
 * <pre>
 * String item = new OrElseTemplate<String>(){
 * <p/>
 *    String run(Transaction t){
 *        return stack1.pop();
 *    }
 * <p/>
 *    String orelscerun(Transaction t){
 *        return stack2.pop();
 *    }
 * }.execute();
 * </pre>
 * <p/>
 * If an exception is thrown in the run block, the block is ended but the orelse block is not started and the exception
 * is propagated.
 * <p/>
 * Does not start a transaction if no transaction is found.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
public abstract class OrElseTemplate<E> {

    private final Transaction tx;

    /**
     * Creates a OrElseTemplate using the transaction in the getThreadLocalTransaction.
     *
     * @throws NullPointerException if no transaction is found.
     */
    public OrElseTemplate() {
        this(getThreadLocalTransaction());
    }

    /**
     * Creates an OrElseTemplate using the provided transaction.
     *
     * @param tx the provided transaction.
     * @throws NullPointerException if is null.
     */
    public OrElseTemplate(Transaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }
        this.tx = tx;
    }

    public abstract E run(Transaction tx);

    public abstract E orelserun(Transaction tx);

    public final E execute() {
        try {
            return run(tx);
        } catch (Retry e) {
            return orelserun(tx);
        }
    }
}
