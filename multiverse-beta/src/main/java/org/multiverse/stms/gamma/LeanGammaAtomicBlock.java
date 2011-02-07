package org.multiverse.stms.gamma;

import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.closures.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransactionPool;

import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransactionContainer;

/**
* An GammaAtomicBlock made for the GammaStm.
*
* This code is generated.
*
* @author Peter Veentjer
*/
public final class LeanGammaAtomicBlock extends AbstractGammaAtomicBlock{
private static final Logger logger = Logger.getLogger(LeanGammaAtomicBlock.class.getName());



    public LeanGammaAtomicBlock(final GammaTransactionFactory transactionFactory) {
        super(transactionFactory);
    }

    @Override
    public GammaTransactionFactory getTransactionFactory(){
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        E result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        int result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        long result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        double result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                return closure.execute(tx);
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        boolean result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
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

        final ThreadLocalTransaction.Container transactionContainer = getThreadLocalTransactionContainer();
        GammaTransactionPool pool = (GammaTransactionPool) transactionContainer.txPool;
        if (pool == null) {
            pool = new GammaTransactionPool();
            transactionContainer.txPool = pool;
        }

        GammaTransaction tx = (GammaTransaction)transactionContainer.tx;
        if(tx == null || !tx.isAlive()){
            tx = null;
        }

        Throwable cause = null;
        try{
            if(tx != null && tx.isAlive()){
                closure.execute(tx);
                return;
            }

            tx = transactionFactory.newTransaction(pool);
            transactionContainer.tx=tx;
            boolean abort = true;
            try {
                do {
                    try {
                        cause = null;
                        closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return;
                    } catch (Retry e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a retry",
                                    transactionConfiguration.familyName));
                            }
                        }
                        tx.awaitUpdate();
                    } catch (SpeculativeConfigurationError e) {
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a speculative configuration error",
                                    transactionConfiguration.familyName));
                            }
                        }

                        abort = false;
                        GammaTransaction old = tx;
                        tx = transactionFactory.upgradeAfterSpeculativeFailure(tx,pool);
                        pool.put(old);
                        transactionContainer.tx = tx;
                    } catch (ReadWriteConflict e) {
                        if(tx.status!=GammaConstants.TX_ABORTED){
                            throw new RuntimeException();
                        }
                        cause = e;
                        if(TRACING_ENABLED){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(format("[%s] Encountered a read or write conflict",
                                    transactionConfiguration.familyName));
                            }
                        }

                        backoffPolicy.delayedUninterruptible(tx.getAttempt());
                    }
                } while (tx.softReset());
            } finally {
                if (abort) {
                    tx.abort();
                }

                pool.put(tx);
                transactionContainer.tx = null;
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }

        if(TRACING_ENABLED){
            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                logger.info(format("[%s] Maximum number of %s retries has been reached",
                    transactionConfiguration.familyName, transactionConfiguration.getMaxRetries()));
            }
        }

        throw new TooManyRetriesException(
            format("[%s] Maximum number of %s retries has been reached",
                transactionConfiguration.getFamilyName(), transactionConfiguration.getMaxRetries()), cause);
        }

   }
