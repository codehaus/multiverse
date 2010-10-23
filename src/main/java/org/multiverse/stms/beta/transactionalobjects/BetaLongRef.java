package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * The transactional object. Atm it is just a reference for an int, more complex stuff will be added again
 * once this project leaves the prototype stage.
 * <p/>
 * remember:
 * it could be that the lock is acquired, but the lockOwner has not been set yet.
 *
 * The whole idea of code generation is that once you are inside a concrete class,
 * polymorphism is needed anymore.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class BetaLongRef
    extends VeryAbstractBetaTransactionalObject
    implements LongRef
{

    //Active needs to be volatile. If not, the both load statements in the load function, can be reordered
    //(the instruction above can jump below the orec.arrive if no write is done)
    private volatile long ___value;

     /**
     * Creates a uncommitted BetaLongRef that should be attached to the transaction (this
     * is not done)
     *
     * @param tx the transaction this BetaLongRef should be attached to.
     * @throws NullPointerException if tx is null.
     */
    public BetaLongRef(BetaTransaction tx){
        super(tx.getConfiguration().stm);
        ___tryLockAndArrive(0, true);
        this.___lockOwner = tx;
    }

    /**
     * Creates a committed BetaLongRef with 0 as initial value.
     *
     * @param stm the BetaStm this reference belongs to.
     * @throws NullPointerException if stm is null.
     */
    public BetaLongRef(BetaStm stm){
        this(stm, (long)0);
    }

    /**
     * Creates a committed BetaLongRef with the given initial value.
     *
     * @param stm the BetaStm this reference belongs to.
     * @param initialValue the initial value
     * @throws NullPointerException is stm is null.
     */
    public BetaLongRef(BetaStm stm, final long initialValue){
        super(stm);

        ___value = initialValue;
        ___version = VERSION_UNCOMMITTED+1;
    }


   @Override
    public final int ___getClassIndex(){
        return 4;
    }

    public final long ___weakRead(){
        return ___value;
    }

    @Override
    public final LongRefTranlocal ___newTranlocal(){
        return new LongRefTranlocal(this);
    }

    @Override
    public final boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, Tranlocal tranlocal){
        return ___load(
            spinCount,
            newLockOwner,
            lockMode,
            (LongRefTranlocal)tranlocal);
    }

    public final boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, LongRefTranlocal tranlocal){
        if(lockMode == LOCKMODE_NONE){
            while (true) {
                //JMM: nothing can jump behind the following statement
                final long firstValue = ___value;
                final long firstVersion = ___version;

                //JMM: the read for the arrive can't jump over the read of the active.
                final int arriveStatus = ___arrive(spinCount);

                if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                    return false;
                }

                //JMM safety:
                //The volatile read of active can't be reordered so that it jump in front of the volatile read of
                //the orec-value when the arrive method is called.
                //An instruction is allowed to jump in front of the write of orec-value, but it is not allowed to
                //jump in front of the read or orec-value (volatile read happens before rule).
                //This means that it isn't possible that a locked value illegally is seen as unlocked.

                if (firstVersion == ___version && firstValue == ___value) {
                    //at this point we are sure that the read was unlocked.

                    tranlocal.version = firstVersion;
                    tranlocal.value = firstValue;
                    tranlocal.oldValue = firstValue;
                    tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
                    return true;
                }

                //we are not lucky, the value has changed. But before retrying, we need to depart if the arrive was
                //not permanent.
                if (arriveStatus == ARRIVE_NORMAL) {
                    ___departAfterFailure();
                }
            }
        }else{
            final boolean commitLock = lockMode == LOCKMODE_COMMIT;

            if(newLockOwner == null){
                throw new PanicError();
            }

            //JMM: no instructions will jump in front of a volatile read. So this stays on top.
            final int arriveStatus = ___tryLockAndArrive(___stm.spinCount, commitLock);
            if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
                return false;
            }

            ___lockOwner = newLockOwner;
            final long value = ___value;

            tranlocal.version = ___version;
            tranlocal.value = value;
            tranlocal.oldValue = value;
            tranlocal.setLockMode(commitLock ? LOCKMODE_COMMIT: LOCKMODE_UPDATE);
            tranlocal.setDepartObligation(arriveStatus == ARRIVE_NORMAL);
            return true;
        }
   }

    @Override
    public final Listeners ___commitDirty(
            final Tranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        final LongRefTranlocal specializedTranlocal = (LongRefTranlocal)tranlocal;

        if(!tranlocal.isDirty()){
            if(tranlocal.getLockMode() != LOCKMODE_NONE){
                ___lockOwner = null;

                if(tranlocal.hasDepartObligation()){
                    ___departAfterReadingAndUnlock();
                }else{
                    ___unlockByReadBiased();
                }
            }else{
                if(tranlocal.hasDepartObligation()){
                    ___departAfterReading();
                }
            }

            pool.put(specializedTranlocal);            
            return null;
        }

        //it is a full blown update (so locked).

        ___value = specializedTranlocal.value;
        ___version = specializedTranlocal.version+1;

        if(___version == -1){
            System.out.println("hello");
        }

        //todo: JMM problem here, the volatile read of ___listeners could jump in front of the volatile write of
        //version, meaning that it could lead to not picking up the listeners that is done after the write. And
        //this could lead to a deadlock.
        Listeners listenersAfterWrite = ___listeners;

        if(listenersAfterWrite != null){
           listenersAfterWrite = ___removeListenersAfterWrite();
        }

        ___lockOwner = null;

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        pool.put(specializedTranlocal);
        return listenersAfterWrite;
    }

    @Override
    public final Listeners ___commitAll(
            final Tranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        final LongRefTranlocal specializedTranlocal = (LongRefTranlocal)tranlocal;

        if(tranlocal.isReadonly()){
            if(tranlocal.getLockMode() != LOCKMODE_NONE){
                ___lockOwner = null;

                if(tranlocal.hasDepartObligation()){
                    ___departAfterReadingAndUnlock();
                }else{
                    ___unlockByReadBiased();
                }
            }else{
                if(tranlocal.hasDepartObligation()){
                    ___departAfterReading();
                }
            }

            pool.put(specializedTranlocal);
            return null;
        }

        //it is a full blown update (so locked).

        ___value = specializedTranlocal.value;
        ___version = specializedTranlocal.version+1;
        ___lockOwner = null;

        //todo: JMM problem here, the volatile read could jump in front of the volatile write of version, meaning
        //that it could lead to not picking up the listeners that is done after the write. And this could lead to
        //a deadlock.
        Listeners listenersAfterWrite = ___listeners;

        if(listenersAfterWrite != null){
           listenersAfterWrite = ___removeListenersAfterWrite();
        }

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        pool.put(specializedTranlocal);
        return listenersAfterWrite;
    }

    @Override
    public final void ___abort(
        final BetaTransaction transaction,
        final Tranlocal tranlocal,
        final BetaObjectPool pool) {

        if(tranlocal.getLockMode() != LOCKMODE_NONE){
            ___lockOwner = null;

            if(!tranlocal.isConstructing()){
                //depart and release the lock. This call is able to deal with readbiased and normal reads.
                ___departAfterFailureAndUnlock();
            }
        }else{
            if(tranlocal.hasDepartObligation()){
                ___departAfterFailure();
            }
        }

        pool.put((LongRefTranlocal)tranlocal);
    }

    @Override
    public void addDeferredValidator(LongPredicate validator){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"addDeferredValidator");
        }

        addDeferredValidator((BetaTransaction)tx, validator);
    }

    @Override
    public void addDeferredValidator(Transaction tx, LongPredicate validator){
        addDeferredValidator((BetaTransaction)tx, validator);
    }

    public void addDeferredValidator(BetaTransaction tx, LongPredicate validator){
        if(tx == null){
            throw new NullPointerException();
        }

        if(validator == null){
            tx.abort();
            throw new NullPointerException();
        }

        LongRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);
        if(write.validators == null){
            write.validators = new LongPredicate[1];
            write.validators[0]=validator;
        }else{
            throw new TodoException();
        }
    }

    @Override
    public void atomicAddDeferredValidator(LongPredicate validator){
        if(validator == null){
            throw new NullPointerException();
        }
        throw new TodoException();
    }

    @Override
    public final long atomicGetAndIncrement(final long amount){
        long result = atomicIncrementAndGet(amount);
        return result - amount;
    }

    @Override
    public final long getAndIncrement(final long amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndIncrement");
        }

        return getAndIncrement((BetaTransaction)tx, amount);
    }

    @Override
    public final long getAndIncrement(final Transaction tx, final long amount){
        return getAndIncrement((BetaTransaction)tx, amount);
    }

    public final long getAndIncrement(final BetaTransaction tx, final long amount){
        LongRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

        long oldValue = write.value;
        write.value+=amount;
        return oldValue;
    }

    @Override
    public final long atomicIncrementAndGet(final long amount){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final long oldValue = ___value;

        if(amount == 0){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        final long newValue = oldValue + amount;
        ___value = newValue;
        ___version++;

        Listeners listeners = ___removeListenersAfterWrite();

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);

        if(listeners!=null){
            listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return newValue;
    }

    @Override
    public final long incrementAndGet(final long amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"incrementAndGet");
        }

        return incrementAndGet((BetaTransaction)tx, amount);
    }

    @Override
    public final long incrementAndGet(
        final Transaction tx,
        final long amount){

        return incrementAndGet((BetaTransaction)tx, amount);
    }

    public final long incrementAndGet(
        final BetaTransaction tx,
        final long amount){

        LongRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

        write.value+=amount;
        return write.value;
    }

    @Override
    public final void increment(){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"increment");
        }

        increment(tx);
    }

    @Override
    public final void increment(final Transaction tx){
        if(tx == null){
            throw new NullPointerException();
        }
        ((BetaTransaction)tx).commute(this,Functions.newIncLongFunction());
    }

    @Override
    public final void increment(final long amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"increment");
        }

        increment(tx, amount);
    }

    @Override
    public final void increment(final Transaction tx, final long amount){
        if(tx == null){
            throw new NullPointerException();
        }

        ((BetaTransaction)tx).commute(this,Functions.newIncLongFunction(amount));
    }

    @Override
    public final void decrement(){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"decrement");
        }

        decrement(tx);
    }

    @Override
    public final void decrement(final Transaction tx){
        if(tx == null){
            throw new NullPointerException();
        }

        ((BetaTransaction)tx).commute(this,Functions.newIncLongFunction(-1));
    }

    @Override
    public final void decrement(final long amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"decrement");
        }

        decrement(tx, amount);
    }

    @Override
    public final void decrement(final Transaction tx, final long amount){
        if(tx == null){
            throw new NullPointerException();
        }

        ((BetaTransaction)tx).commute(this,Functions.newIncLongFunction(-amount));
    }
    
    @Override
    public final void ensure(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"ensure");
        }

        ensure((BetaTransaction)tx);
    }

    @Override
    public final void ensure(Transaction tx){
        ensure((BetaTransaction)tx);
    }

    public final void ensure(BetaTransaction tx){
        tx.openForRead(this, LOCKMODE_UPDATE);
    }

    @Override
    public final boolean tryEnsure(){
        Transaction tx = getThreadLocalTransaction();

        if(tx!=null){
            throw new TransactionRequiredException(getClass(),"tryEnsure");
        }

        return tryEnsure((BetaTransaction)tx);
    }

    @Override
    public final boolean tryEnsure(final Transaction tx){
        return tryEnsure((BetaTransaction)tx);
    }

    public final boolean tryEnsure(BetaTransaction tx){
        return tx.tryLock(this, LOCKMODE_UPDATE);
    }

    @Override
    public final void deferredEnsure(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"deferredEnsure");
        }

        deferredEnsure((BetaTransaction)tx);
    }

    @Override
    public final void deferredEnsure(final Transaction tx){
        deferredEnsure((BetaTransaction)tx);
    }

    public final void deferredEnsure(final BetaTransaction tx){
        if(tx == null){
            throw new NullPointerException();
        }

        tx.materializeConflict(this);
    }

    @Override
    public final void privatize(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"privatize");
        }

        privatize((BetaTransaction)tx);
    }

    @Override
    public final void privatize(Transaction tx){
        privatize((BetaTransaction)tx);
    }

    public final void privatize(BetaTransaction tx){
        tx.openForRead(this, LOCKMODE_COMMIT);
    }

    @Override
    public final boolean tryPrivatize(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"tryPrivatize");
        }

        return tryPrivatize((BetaTransaction)tx);
    }

    @Override
    public final boolean tryPrivatize(Transaction tx){
        return tryPrivatize((BetaTransaction)tx);
    }

    public final boolean tryPrivatize(BetaTransaction tx){
        return tx.tryLock(this, LOCKMODE_COMMIT);
    }

    @Override
    public final void commute(LongFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"commute");
        }

        commute((BetaTransaction)tx, function);
    }

    @Override
    public final void commute(final Transaction tx, final LongFunction function){
        commute((BetaTransaction)tx, function);
    }

    public final void commute(BetaTransaction tx,LongFunction function){
        tx.commute(this, function);
    }

    @Override
    public final long atomicAlterAndGet(final LongFunction function){
        return atomicAlter(function, false);
    }

    @Override
    public final long alterAndGet(final LongFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"alterAndGet");
        }

        return alterAndGet((BetaTransaction)tx, function);
    }

    @Override
    public final long alterAndGet(final Transaction tx,final LongFunction function){
        return alterAndGet((BetaTransaction)tx, function);
    }

    public final long alterAndGet(final BetaTransaction tx,final LongFunction function){
        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        LongRefTranlocal write
            = (LongRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

        boolean abort = true;
        try{
            write.value = function.call(write.value);
            abort = false;
        }finally{
            if(abort){
                tx.abort();
            }
        }
        return write.value;
    }

    @Override
    public final long atomicGetAndAlter(final LongFunction function){

        return atomicAlter(function,true);
    }

    private long atomicAlter(final LongFunction function,final boolean returnOld){
        if(function == null){
            throw new NullPointerException("Function can't be null");
        }

        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final long oldValue = ___value;
        long newValue;
        boolean abort = true;
        try{
            newValue = function.call(oldValue);
            abort = false;
        }finally{
            if(abort){
                ___departAfterFailureAndUnlock();
            }
        }

        if(oldValue == newValue){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return oldValue;
        }


        ___value = newValue;
        ___version++;

        Listeners listeners = ___removeListenersAfterWrite();

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);

        if(listeners!=null){
           listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return returnOld ? oldValue : newValue;
    }

    @Override
    public final long getAndAlter(final LongFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndAlter");
        }

        return getAndAlter((BetaTransaction)tx, function);
    }

    @Override
    public final long getAndAlter(final Transaction tx,final LongFunction function){
        return getAndAlter((BetaTransaction)tx, function);
    }

    public final long getAndAlter(final BetaTransaction tx,final LongFunction function){
        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        LongRefTranlocal write
            = (LongRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

        final long oldValue = write.value;
        boolean abort = true;
        try{
            write.value = function.call(write.value);
            abort  = false;
        }finally{
            if(abort){
                tx.abort();
            }
        }
        return oldValue;
    }

    @Override
    public final boolean atomicCompareAndSet(final long expectedValue,final long newValue){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final long currentValue = ___value;

        if(currentValue != expectedValue){
            ___departAfterFailureAndUnlock();
            return false;
        }

        if(expectedValue == newValue){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return true;
        }

        ___value = newValue;
        ___version++;
        Listeners listeners = ___removeListenersAfterWrite();

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);

        if(listeners!=null){
            listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return true;
    }

    @Override
    public final long getAndSet(final long value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndSet");
        }

        return getAndSet((BetaTransaction)tx, value);
    }

    public final long set(final long value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"set");
        }

        return set((BetaTransaction)tx, value);
    }

    @Override
    public final long get(){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"get");
        }

        return get((BetaTransaction)tx);
    }

    @Override
    public final long get(final Transaction tx){
        return get((BetaTransaction)tx);
    }

    public final long get(final BetaTransaction transaction){
        return transaction.openForRead(this, LOCKMODE_NONE).value;
    }

    @Override
    public final long atomicGet(){
        int attempt = 1;
        do{
            if(!___hasCommitLock()){

                long read = ___value;

                if(!___hasCommitLock()){
                    return read;
                }
            }
            ___stm.defaultBackoffPolicy.delayedUninterruptible(attempt);
            attempt++;
        }while(attempt<=___stm.spinCount);

        throw new LockedException();
    }

    @Override
    public final long atomicWeakGet(){
       return ___value;
    }

    @Override
    public final long atomicSet(final long newValue){
        atomicGetAndSet(newValue);
        return newValue;
    }

    @Override
    public final long atomicGetAndSet(final long newValue){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final long oldValue = ___value;

        if(oldValue == newValue){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return newValue;
        }

        ___value = newValue;
        ___version++;

        Listeners listeners = ___removeListenersAfterWrite();

        ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);

        if(listeners != null){
            BetaObjectPool pool = getThreadLocalBetaObjectPool();
            listeners.openAll(pool);
        }

        return oldValue;
    }

    @Override
    public final long set(Transaction tx, long value){
        return set((BetaTransaction)tx, value);
    }

    public final long set(final BetaTransaction tx,final long value){
        tx.openForWrite(this, LOCKMODE_NONE).value = value;
        return value;
    }

    @Override
    public final long getAndSet(final Transaction tx,final long value){
        return getAndSet((BetaTransaction)tx, value);
    }

    public final long getAndSet(final BetaTransaction tx,final long value){
        LongRefTranlocal write = tx.openForWrite(this, LOCKMODE_NONE);
        long oldValue = write.value;
        write.value = value;
        return oldValue;
    }

    @Override
    public final void await(long value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"await");                                            
        }

        await((BetaTransaction)tx, value);
    }

    @Override
    public final void await(final Transaction tx,final long value){
        await((BetaTransaction)tx, value);
    }

    public final void await(final BetaTransaction tx,final long value){
        LongRefTranlocal read = tx.openForRead(this,LOCKMODE_NONE);
        if(read.value != value){
            tx.retry();
        }
    }

    @Override
    public final void await(final LongPredicate predicate){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"await");
        }

        await((BetaTransaction)tx, predicate);
    }

    @Override
    public final void await(final Transaction tx, LongPredicate predicate){
        await((BetaTransaction)tx, predicate);
    }

    public final void await(final BetaTransaction tx, LongPredicate predicate){
        if(tx == null){
            throw new NullPointerException();
        }

        if(predicate == null){
            tx.abort();
            throw new NullPointerException();
        }

        boolean abort = true;
        try{
            final long value = tx.openForRead(this, LOCKMODE_NONE).value;
            boolean result = predicate.evaluate(value);
            abort = false;
            if(!result){
                tx.retry();
            }
        }finally{
            if(abort){
                tx.abort();
            }
        }
    }

    @Override
    public String toDebugString(){
        return String.format("Ref{orec=%s, version=%s, value=%s, hasListeners=%s)",
            ___toOrecString(),___version,___value, ___listeners!=null);
    }
}
