package org.multiverse.api;

import org.multiverse.api.backoff.BackoffPolicy;
import org.multiverse.api.commitlock.CommitLockPolicy;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of the builder design pattern to create a {@link TransactionFactory}. This is the place to be
 * for transaction configuration. This approach also gives the freedom to access implementation specific
 * setters by implementing and extending the {@link TransactionFactoryBuilder} interface.
 * <p/>
 * <h2>Usage</h2>
 * In most cases the TransactionFactoryBuilder will be dropped once the TransactionFactory
 * is created (the TransactionFactory is the one you want to store for later use).
 * <p/>
 * <h2>Chained methods</h2>
 * It is important that the returned TransactionFactoryBuilder is used on the 'set' method because TransactionBuilders
 * are immutable. If you don't do this, your transactions don't get the properties you want them to have.
 * <p/>
 * <h2>Transaction familyName</h2>
 * This is purely used for debugging purposes. In the instrumentation the familyname is determined by the
 * full method signature including the owning class.
 * <p/>
 * <h2>Default configuration</h2>
 * Default a TransactionFactoryBuilder will be configured with:
 * <ol>
 * <li><b>readonly</b>false</li>
 * <li><b>automatic read tracking</b>false</li>
 * <li><b>familyName</b> null</li>
 * <li><b>maxRetryCount</b> 1000</li>
 * </ol>
 * <p/>
 * The big advantage to a builder compared to just adding a big load of parameters or storing these parameters in a
 * datastructure (so object without logic) is that the Stm implementation has a lot more room for adding custom
 * parameters and stm-internal transaction-family related dependencies.
 * <p/>
 * TransactionBuilders are immutable and therefor thread safe to use.
 *
 * @author Peter Veentjer.
 */
public interface TransactionFactoryBuilder<T extends Transaction, B extends TransactionFactoryBuilder> {

    /**
     * Creates a new {@link TransactionFactoryBuilder} based on the this TransactionFactoryBuilder but now
     * configured with the provided familyName.
     * <p/>
     * The transaction familyName is useful for a lot of reasons. It can be used for identification in logging but also
     * can be used to make optimizations based on the transaction familyName. A stm could decide to return optimized
     * transaction implementations for example.
     *
     * @param familyName the familyName of the transaction.
     * @return the new TransactionFactoryBuilder
     */
    B setFamilyName(String familyName);

    /**
     * Creates a new {@link TransactionFactoryBuilder} based on the this TransactionFactoryBuilder but now
     * configured with the readonly setting. A readonly transaction normally is a lot faster than an update
     * transaction and it also provides protection against unwanted changes.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     *
     * @param readonly true if the transaction should be readonly, false otherwise.
     * @return the new TransactionFactoryBuilder
     */
    B setReadonly(boolean readonly);

    /**
     * If the transaction should automatically track all reads that have been done. This is needed for blocking
     * operations, but also for other features like writeskew detection.
     * <p/>
     * If this property is set, the stm will not speculate on this property anymore.
     *
     * @param automaticReadTracking true if readtracking enabled, false otherwise.
     * @return the new TransactionFactoryBuilder
     */
    B setAutomaticReadTracking(boolean automaticReadTracking);

    /**
     * Sets if the transaction can be interrupted while doing blocking operations.
     *
     * @param interruptible if the transaction can be interrupted while doing blocking operations.
     * @return the new TransactionFactoryBuilder
     */
    B setInterruptible(boolean interruptible);

    B setCommitLockPolicy(CommitLockPolicy commitLockPolicy);


    /**
     * With the speculative configuration enabled, the stm is allowed to determine optimal settings
     * for transactions. Some behavior like readonly or the need for tracking reads can be determined
     * runtime. The system can start with a readonly non readtracking transaction and upgrade to an
     * update or a read tracking once a write or retry happens.
     * <p/>
     * The value defaults to true and in most cases is the best setting.
     *
     * @param newValue indicates if speculative configuration should be enabled.
     * @return the new TransactionFactoryBuilder
     */
    B setSpeculativeConfigurationEnabled(boolean newValue);

    /**
     * If writeskew problem is allowed to happen. Defaults to true and can have a big impact on performance (the
     * complete read set needs to be validated and not just the writes). So disable it wisely.
     *
     * @param allowWriteSkew indicates if writeSkew problem is allowed.
     * @return the new TransactionFactoryBuilder
     */
    B setAllowWriteSkewProblem(boolean allowWriteSkew);

    /**
     * Checks if the quick release on locks is enabled. When a transaction commits, it
     * needs to acquire the writelocks. If quick release is disabled, first all writes are
     * executed before any lock is released. With quick release enabled, the lock on the
     * transactional object is released as soon as the write is done.
     *
     * @param enabled
     * @return the created TransactionFactoryBuilder
     */
    B setQuickReleaseEnabled(boolean enabled);

    /**
     * Sets the new backoff policy. Policy is used to backoff when a transaction conflicts with another transaction.
     * See the {@link BackoffPolicy} for more information.
     *
     * @param backoffPolicy the backoff policy to use.
     * @return the new TransactionFactoryBuilder
     * @throws NullPointerException if backoffPolicy is null.
     */
    B setBackoffPolicy(BackoffPolicy backoffPolicy);

    B setTimeout(long timeout, TimeUnit unit);

    /**
     * Sets the the maximum count a transaction can be retried. The default is 1000.
     *
     * @param retryCount the maximum number of times a transaction can be tried.
     * @return the new TransactionFactoryBuilder
     */
    B setMaxRetryCount(int retryCount);

    /**
     * Builds a {@link TransactionFactory} with the provided configuration.
     *
     * @return the started Transaction.
     * @throws IllegalStateException if this TransactionFactoryBuilder is not configured correctly and therefor the
     *                               TransactionFactory can't be created.
     */
    TransactionFactory<T> build();

    // B setLoggingEnabled(boolean loggingEnabled);
}
