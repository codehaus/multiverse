package org.multiverse.api;

/**
 * Using the ProgrammaticReferenceFactory it is possible to get access to the
 * 'manual' instrumented reference: ProgrammaticReference. This reference
 * can safely be used without relying on instrumentation. It also provides
 * access to methods not available in the normal instrumented classes.
 * <p/>
 * This functionality specially is added to integrate Multiverse in environments
 * where you don't want to rely on instrumentation but just want to have
 * a 'managed reference.
 * <p/>
 * Methods are thread safe.
 *
 * @author Peter Veentjer
 */
public interface ProgrammaticReferenceFactory {

    /**
     * Creates a new ProgrammaticReference with the provided value. If a
     * transaction already is running, it will lift on that transaction. If no
     * transaction is running, then this method will be run in its own transaction.
     * <p/>
     * If the ProgrammaticReference leaves the transaction it was created in before
     * that transaction commits, you will get the
     * {@link org.multiverse.api.exceptions.UncommittedReadConflict}.
     *
     * @param value the value stored in the reference (is allowed to be null).
     * @param <E>
     * @return the created ProgrammaticReference.
     */
    <E> ProgrammaticReference<E> create(E value);

    /**
     * Creates a new ProgrammaticReference with the provided value and the
     * 
     *
     * @param t
     * @param value
     * @param <E>
     * @return
     * @throws NullPointerException if t is null.
     */
    <E> ProgrammaticReference<E> create(Transaction t, E value);
}
