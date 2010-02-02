package org.multiverse.api;

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
 * The same TransactionFactory instance should not be used as general purpose transaction factory for the entire
 * system. Each location
 * that needs transactions, wants to receive its own TransactionFactory instance because it can be optimization
 * for that specific situation (readonly vs update, automatic readtracking enabled and disabled etc). Besides these
 * static optimizations, the TransactionFactories also are able to do runtime optimizations (like selecting better
 * suiting transaction implementations) based on previous executions. For this to work, the transaction familyName
 * needs to be set.
 * <p/>
 * <h2>Default configuration</h2>
 * Default a TransactionFactoryBuilder will be configured with:
 * <ol>
 * <li><b>readonly</b> true</li>
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
     *
     * @param readonly true if the transaction should be readonly, false otherwise.
     * @return the new TransactionFactoryBuilder
     */
    B setReadonly(boolean readonly);

    /**
     * Sets the the maximum count a transaction can be retried. The default is 1000.
     *
     * @param retryCount the maximum number of times a transaction can be tried.
     * @return the new TransactionFactoryBuilder
     */
    B setMaxRetryCount(int retryCount);

    /**
     * If the transaction should automatically track all reads that have been done. This is needed for blocking
     * operations, but also for other features like writeskew detection.
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

    /**
     * Work around for making sure that on some locations there is a choice
     * for the correct transaction length. Constructors are not able to deal with
     * retrying tx.
     *
     * @param smartTxlengthSelector indicates if smartTxlength selection should be used.
     *
     * @return the new TransactionFactoryBuilder
     */
    B setSmartTxLengthSelector(boolean smartTxlengthSelector);

    /**
     * If writeskew prevention should be enabled.
     *
     * @param preventWriteSkew  indicates if writeSkews should be prevented.
     * @return the new TransactionFactoryBuilder
     */
    B setPreventWriteSkew(boolean preventWriteSkew);

    /**
     * Builds a {@link TransactionFactory} with the provided configuration.
     *
     * @return the started Transaction.
     * @throws IllegalStateException if the configuration for creating a transaction factory is not valid.
     */
    TransactionFactory<T> build();

    // B setLoggingEnabled(boolean loggingEnabled);

    //B setCommitLockPolicy(CommitLockPolicy commitLockPolicy);


}
