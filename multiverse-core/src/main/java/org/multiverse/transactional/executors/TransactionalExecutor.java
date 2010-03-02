package org.multiverse.transactional.executors;

import org.multiverse.annotations.TransactionalObject;

import java.util.concurrent.Executor;

/**
 * The transactional version of the {@link Executor}.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public interface TransactionalExecutor extends Executor {
}
