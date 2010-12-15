package org.multiverse.stms.beta;

import org.multiverse.api.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.closures.*;
import org.multiverse.stms.beta.transactions.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.*;

/**
 * An AbstractBetaAtomicBlock made for the BetaStm.
 *
 * This code is generated.
 *
 * @author Peter Veentjer
 */
public final class FatBetaAtomicBlock extends AbstractBetaAtomicBlock{
    private static final Logger logger = Logger.getLogger(FatBetaAtomicBlock.class.getName());



    private final PropagationLevel propagationLevel;

    public FatBetaAtomicBlock(final BetaTransactionFactory transactionFactory) {
        super(transactionFactory);
        this.propagationLevel = transactionConfiguration.propagationLevel;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private <E> E execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicClosure<E> closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        E result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private  int execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicIntClosure closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        int result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private  long execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicLongClosure closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        long result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private  double execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicDoubleClosure closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        double result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        return closure.execute(tx);
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    return closure.execute(tx);
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    return closure.execute(null);
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        return execute(tx, transactionContainer, closure);
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            return execute(tx, transactionContainer, closure);
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    return closure.execute(tx);
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private  boolean execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicBooleanClosure closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        boolean result = closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return result;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
            switch (propagationLevel) {
                case Requires:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level and no transaction found, starting a new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        execute(tx, transactionContainer, closure);
                        return;
                    } else {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Requires' propagation level, and existing transaction [%s] found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        closure.execute(tx);
                        return;
                    }
                case Mandatory:
                    if (tx == null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Mandatory' propagation level, and no transaction is found",
                                        transactionConfiguration.familyName));
                            }
                        }
                        throw new TransactionRequiredException(
                            format("No transaction is found for atomicblock '%s' with 'Mandatory' propagation level",
                                transactionConfiguration.familyName));
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Mandatory' propagation level and transaction [%s] found",
                                    transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                        }
                    }

                    closure.execute(tx);
                    return;
                case Never:
                    if (tx != null) {
                        if (___TracingEnabled) {
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'Never' propagation level, but transaction [%s] is found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        throw new TransactionNotAllowedException(
                            format("No transaction is allowed for atomicblock '%s' with propagation level 'Never'"+
                                ", but transaction '%s' was found",
                                transactionConfiguration.familyName, tx.getConfiguration().getFamilyName())
                        );
                    }

                    if (___TracingEnabled) {
                        if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                            logger.info(
                                format("[%s] Has 'Never' propagation level and no transaction is found",
                                    transactionConfiguration.familyName));
                        }
                    }

                    closure.execute(null);
                    return;
                case RequiresNew:
                    if (tx == null) {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagation level and no transaction is found, starting new transaction",
                                        transactionConfiguration.familyName));
                            }
                        }

                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        execute(tx, transactionContainer, closure);
                        return;
                    } else {
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }

                        BetaTransaction suspendedTransaction = tx;
                        tx = transactionFactory.newTransaction(transactionContainer.transactionPool);
                        transactionContainer.transaction = tx;
                        try {
                            execute(tx, transactionContainer, closure);
                            return;
                        } finally {
                            transactionContainer.transaction = suspendedTransaction;
                        }
                    }
                case Supports:
                    if(___TracingEnabled){
                        if(tx!=null){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }else{
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Has 'RequiresNew' propagationLevel and existing transaction [%s] was found",
                                        transactionConfiguration.familyName, tx.getConfiguration().getFamilyName()));
                            }
                        }
                    }

                    closure.execute(tx);
                    return;
                default:
                    throw new IllegalStateException();
            }
        }catch(RuntimeException e){
            throw e;
        }catch(Exception e){
            throw new InvisibleCheckedException(e);
        }
   }

   private  void execute(
       BetaTransaction tx, final ThreadLocalTransaction.Container transactionContainer, final AtomicVoidClosure closure)throws Exception{

       try{
            boolean abort = true;

            try {
                do {
                    try {
                        closure.execute(tx);
                        tx.commit();
                        abort = false;
                        return;
                    } catch (Retry e) {
                        abort = false;
                        if(___TracingEnabled){
                            if (transactionConfiguration.getTraceLevel().isLogableFrom(TraceLevel.Course)) {
                                logger.info(
                                    format("[%s] Encountered a retry",
                                        transactionConfiguration.familyName));
                            }
                        }                        
                    } catch (SpeculativeConfigurationError e) {
                        abort = false;
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
