package org.multiverse.stms.alpha;

import org.multiverse.api.Stm;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.commitlock.CommitLockPolicy;
import org.multiverse.utils.profiling.ProfileRepository;
import org.multiverse.utils.profiling.ProfilerAware;
import org.multiverse.utils.restartbackoff.RestartBackoffPolicy;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default {@link Stm} implementation that provides the most complete set of features. Like retry/orelse, profiling,
 * etc. It can be configured through the {@link AlphaStmConfig}.
 * <p/>
 * <h3>Statistics</h3> This implementation can use {@link org.multiverse.utils.profiling.ProfileRepository}. This choice
 * needs to be made when the STM is constructed, so that the JIT can remove calls to the Profiler completely if a null
 * value is passed. The JIT is able to completely remove the following:
 * <pre>
 * if(profiler!=null){
 *      profiler.incSomeCounter();
 * }
 * </pre>
 * So if you are not using the profiler, you don't need to pay for it.
 * <p/>
 * The instrumentation is added directly to the code. Although it is less pretty, adding some form of external mechanism
 * to add this functionality is going to complicate matters (not at least deployment issues).
 * <p/>
 * <h3>Logging</h3> Logging to java.logging can be enabled through the constructor.
 * <p/>
 * The logging can be completely removed by the JIT if the loggingPossible flag is set to false. No additional checks
 * are done.. so you don't need to pay the price for it if you don't use it.
 *
 * @author Peter Veentjer.
 */
public final class AlphaStm implements Stm, ProfilerAware {

    private final static Logger logger = Logger.getLogger(AlphaStm.class.getName());

    private final Clock clock;

    private final ProfileRepository profiler;

    private final boolean loggingPossible;

    private final AtomicLong logIdGenerator;

    private final CommitLockPolicy commitLockPolicy;

    private final RestartBackoffPolicy restartBackoffPolicy;

    private final UpdateTransactionDependencies updateTransactionDependencies;

    private final ReadonlyAlphaTransactionDependencies readonlyAlphaTransactionDependencies;

    public static AlphaStm createFast() {
        return new AlphaStm(AlphaStmConfig.createFastConfig());
    }

    public static AlphaStm createDebug() {
        return new AlphaStm(AlphaStmConfig.createDebugConfig());
    }

    /**
     * Creates a new AlphaStm with the AlphaStmConfig.createDebugConfig as configuration.
     */
    public AlphaStm() {
        this(AlphaStmConfig.createDebugConfig());
    }

    /**
     * Creates a new AlphaStm with the provided configuration.
     *
     * @param config the provided config.
     * @throws NullPointerException  if config is null.
     * @throws IllegalStateException if the provided config is invalid.
     */
    public AlphaStm(AlphaStmConfig config) {
        if (config == null) {
            throw new NullPointerException();
        }

        config.ensureValid();

        this.profiler = config.profiler;
        this.clock = config.clock;
        //the abstracttransaction requires the clock to be at least 1, requirement from the
        //abstracttransaction.
        if (clock.getTime() == 0) {
            clock.tick();
        }
        this.loggingPossible = config.loggingPossible;
        this.logIdGenerator = loggingPossible ? new AtomicLong() : null;
        this.commitLockPolicy = config.commitLockPolicy;
        this.restartBackoffPolicy = config.restartBackoffPolicy;

        this.updateTransactionDependencies = new UpdateTransactionDependencies(
                clock,
                restartBackoffPolicy,
                commitLockPolicy,
                profiler);
        this.readonlyAlphaTransactionDependencies = new ReadonlyAlphaTransactionDependencies(
                clock,
                restartBackoffPolicy,
                profiler);

        logger.info("Created a new AlphaStm instance");
    }

    /**
     * Returns the current WriteSetLockPolicy. Returned value will never be null.
     *
     * @return the current WriteSetLockPolicy.
     */
    public CommitLockPolicy getAtomicObjectLockPolicy() {
        return commitLockPolicy;
    }


    /**
     * Returns the current RestartBackoffPolicy. Returned value will never be null.
     *
     * @return
     */
    public RestartBackoffPolicy getRestartBackoffPolicy() {
        return restartBackoffPolicy;
    }

    /**
     * Returns the DefaultStmStatistics or null if the Stm is running without statistics.
     *
     * @return return the TL2StmStatistics.
     */
    public ProfileRepository getProfiler() {
        return profiler;
    }

    @Override
    public AlphaTransaction startUpdateTransaction(String familyName) {
        if (loggingPossible) {
            return new LoggingUpdateAlphaTransaction(
                    updateTransactionDependencies,
                    familyName,
                    logIdGenerator.incrementAndGet(),
                    Level.FINE);
        } else {
            return new UpdateAlphaTransaction(
                    updateTransactionDependencies,
                    familyName);
        }
    }

    @Override
    public AlphaTransaction startReadOnlyTransaction(String familyName) {
        if (loggingPossible) {
            return new LoggingReadonlyAlphaTransaction(
                    readonlyAlphaTransactionDependencies,
                    familyName,
                    logIdGenerator.incrementAndGet(),
                    Level.FINE);
        } else {
            return new ReadonlyAlphaTransaction(
                    readonlyAlphaTransactionDependencies,
                    familyName);
        }
    }

    @Override
    public long getTime() {
        return clock.getTime();
    }
}
