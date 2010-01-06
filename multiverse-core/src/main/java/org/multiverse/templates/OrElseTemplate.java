package org.multiverse.templates;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.RetryError;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A template for the 'orelse' functionality. Example:
 * <pre>
 * String item = new OrElseTemplate<String>(){
 * <p/>
 *    String run(Transaction t){
 *        return stack1.pop();
 *    }
 * <p/>
 *    String orelserun(Transaction t){
 *        return stack2.pop();
 *    }
 * }.execute();
 * </pre>
 * <p/>
 * If an exception is thrown in the run block, the block is ended but the
 * orelse block is not started and the exception is propagated.
 * <p/>
 * Does not start a transaction if no transaction is found.
 *
 * @author Peter Veentjer.
 * @param <E>
 */
public abstract class OrElseTemplate<E> {
    private final Transaction t;

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
     * @param t the provided transaction.
     * @throws NullPointerException if is null.
     */
    public OrElseTemplate(Transaction t) {
        if (t == null) throw new NullPointerException();
        this.t = t;
    }

    public abstract E run(Transaction t);

    public abstract E orelserun(Transaction t);

    public final E execute() {
        t.startOr();
        boolean endOr = true;
        try {
            return run(t);
        } catch (RetryError e) {
            endOr = false;
            t.endOrAndStartElse();
            return orelserun(t);
        } finally {
            if (endOr) {
                t.endOr();
            }
        }
    }
}
