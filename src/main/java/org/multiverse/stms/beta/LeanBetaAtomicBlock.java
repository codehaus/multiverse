package org.multiverse.stms.beta;

import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.closures.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.sensors.TransactionSensor;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransactionContainer;

/**
 * An AbstractBetaAtomicBlock made for the BetaStm.
 *
 * This code is generated.
 *
 * @author Peter Veentjer
 */
public final class LeanBetaAtomicBlock extends AbstractBetaAtomicBlock{
    private static final Logger logger = Logger.getLogger(LeanBetaAtomicBlock.class.getName());



    public LeanBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        super(transactionFactory);
    }

    @Override
    public BetaTransactionFactory getTransactionFactory(){
        return transactionFactory;
    }

    @Override
    public <E> E executeChecked(
        final AtomicClosure<E> closure)throws Exception{

        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public <E> E execute(final AtomicClosure<E> closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        E result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  int executeChecked(
        final AtomicIntClosure closure)throws Exception{

        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  int execute(final AtomicIntClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        int result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  long executeChecked(
        final AtomicLongClosure closure)throws Exception{

        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  long execute(final AtomicLongClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        long result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  double executeChecked(
        final AtomicDoubleClosure closure)throws Exception{

        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  double execute(final AtomicDoubleClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        double result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  boolean executeChecked(
        final AtomicBooleanClosure closure)throws Exception{

        try{
            return execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  boolean execute(final AtomicBooleanClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        boolean result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
     @Override
    public  void executeChecked(
        final AtomicVoidClosure closure)throws Exception{

        try{
            execute(closure);
        }catch(InvisibleCheckedException e){
            throw e.getCause();
        }
    }

    @Override
    public  void execute(final AtomicVoidClosure closure){

        if(closure == null){
            throw new NullPointerException();
        }

        ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        BetaTransaction tx = (BetaTransaction)transactionContainer.transaction;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        try{
            if(tx != null && tx.isAlive()){
                closure.execute(tx);
                return;
            }

            tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
            transactionContainer.transaction=tx;
            boolean abort = true;

            try {
                do {
                    try {
                        closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return;
                    } catch (Retry e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }
                        waitForChange(tx);
                    } catch (SpeculativeConfigurationError e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a speculative configuration error",
                                        transactionConfiguration.familyName));
                            }
                        }

                        BetaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,transactionContainer.transactionPool);
                        transactionContainer.transactionPool.putBetaTransaction(old);
                        transactionContainer.transaction = tx;                        
                    } catch (ReadWriteConflict e) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a read or write conflict",
                                        transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    } 
                } while (tx.softReset());
            } finally {
                if(___ProfilingEnabled){
                    TransactionSensor sensor = transactionConfiguration.transactionSensor;
                    if(sensor != null){
                        sensor.signalExecution(tx.getAttempt(), !abort);
                    }
                }

                if (abort) {
                    tx.abort();
                }

                transactionContainer.transactionPool.putBetaTransaction(tx);
                transactionContainer.transaction = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

            if(___TracingEnabled){
                if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                    logger.info(
                        format("[%s] Too many retries, a maximum of %s is allowed",
                            transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
                }
            }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()));

    }
   }
