package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.*;
import org.multiverse.api.*;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.predicates.*;
import org.multiverse.api.references.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactions.*;

import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.*;

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
public final class BetaIntRef
    extends VeryAbstractBetaTransactionalObject
    implements IntRef
{

    //Active needs to be volatile. If not, the both load statements in the load function, can be reordered
    //(the instruction above can jump below the orec.arrive if no write is done)
    private volatile int ___value;

     /**
     * Creates a uncommitted BetaIntRef that should be attached to the transaction (this
     * is not done)
     *
     * @param tx the transaction this BetaIntRef should be attached to.
     * @throws NullPointerException if tx is null.
     */
    public BetaIntRef(BetaTransaction tx){
        super(tx.getConfiguration().stm);
        ___tryLockAndArrive(0, true);
        this.___lockOwner = tx;
    }

    /**
     * Creates a committed BetaIntRef with 0 as initial value.
     *
     * @param stm the BetaStm this reference belongs to.
     * @throws NullPointerException if stm is null.
     */
    public BetaIntRef(BetaStm stm){
        this(stm, (int)0);
    }

    /**
     * Creates a committed BetaIntRef with the given initial value.
     *
     * @param stm the BetaStm this reference belongs to.
     * @param initialValue the initial value
     * @throws NullPointerException is stm is null.
     */
    public BetaIntRef(BetaStm stm, final int initialValue){
        super(stm);

        ___value = initialValue;
        ___version = VERSION_UNCOMMITTED+1;
    }


   @Override
    public final int ___getClassIndex(){
        return 1;
    }

    public final int ___weakRead(){
        return ___value;
    }

    @Override
    public final BetaIntRefTranlocal ___newTranlocal(){
        return new BetaIntRefTranlocal(this);
    }

    @Override
    public final boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, BetaTranlocal tranlocal){
        return ___load(
            spinCount,
            newLockOwner,
            lockMode,
            (BetaIntRefTranlocal)tranlocal);
    }

    public final boolean ___load(int spinCount, BetaTransaction newLockOwner, int lockMode, BetaIntRefTranlocal tranlocal){
        if(lockMode == LOCKMODE_NONE){
            while (true) {
                //JMM: nothing can jump behind the following statement
                final int firstValue = ___value;
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
            final int value = ___value;

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
            final BetaTranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        final BetaIntRefTranlocal specializedTranlocal = (BetaIntRefTranlocal)tranlocal;

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

        ___departAfterUpdateAndUnlock();
        pool.put(specializedTranlocal);
        return listenersAfterWrite;
    }

    @Override
    public final Listeners ___commitAll(
            final BetaTranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        final BetaIntRefTranlocal specializedTranlocal = (BetaIntRefTranlocal)tranlocal;

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

        ___departAfterUpdateAndUnlock();
        pool.put(specializedTranlocal);
        return listenersAfterWrite;
    }

    @Override
    public final void ___abort(
        final BetaTransaction transaction,
        final BetaTranlocal tranlocal,
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

        pool.put((BetaIntRefTranlocal)tranlocal);
    }


    @Override
    public final int atomicGetAndIncrement(final int amount){
        int result = atomicIncrementAndGet(amount);
        return result - amount;
    }

    @Override
    public final int getAndIncrement(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndIncrement");
        }

        return getAndIncrement((BetaTransaction)tx, amount);
    }

    @Override
    public final int getAndIncrement(final Transaction tx, final int amount){
        return getAndIncrement((BetaTransaction)tx, amount);
    }

    public final int getAndIncrement(final BetaTransaction tx, final int amount){
        BetaIntRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

        int oldValue = write.value;
        write.value+=amount;
        return oldValue;
    }

    @Override
    public final int atomicIncrementAndGet(final int amount){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final int oldValue = ___value;

        if(amount == 0){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return oldValue;
        }

        final int newValue = oldValue + amount;
        ___value = newValue;
        ___version++;

        Listeners listeners = ___removeListenersAfterWrite();

        ___departAfterUpdateAndUnlock();

        if(listeners!=null){
            listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return newValue;
    }

    @Override
    public final int incrementAndGet(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"incrementAndGet");
        }

        return incrementAndGet((BetaTransaction)tx, amount);
    }

    @Override
    public final int incrementAndGet(
        final Transaction tx,
        final int amount){

        return incrementAndGet((BetaTransaction)tx, amount);
    }

    public final int incrementAndGet(
        final BetaTransaction tx,
        final int amount){

        BetaIntRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

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
        ((BetaTransaction)tx).commute(this,Functions.newIncIntFunction());
    }

    @Override
    public final void increment(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"increment");
        }

        increment(tx, amount);
    }

    @Override
    public final void increment(final Transaction tx, final int amount){
        if(tx == null){
            throw new NullPointerException();
        }

        ((BetaTransaction)tx).commute(this,Functions.newIncIntFunction(amount));
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

        ((BetaTransaction)tx).commute(this,Functions.newIncIntFunction(-1));
    }

    @Override
    public final void decrement(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"decrement");
        }

        decrement(tx, amount);
    }

    @Override
    public final void decrement(final Transaction tx, final int amount){
        if(tx == null){
            throw new NullPointerException();
        }

        ((BetaTransaction)tx).commute(this,Functions.newIncIntFunction(-amount));
    }

    @Override
    public final void acquireWriteLock(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"ensure");
        }

        acquireWriteLock((BetaTransaction)tx);
    }

    @Override
    public final void acquireWriteLock(Transaction tx){
        acquireWriteLock((BetaTransaction)tx);
    }

    public final void acquireWriteLock(BetaTransaction tx){
        tx.openForRead(this, LOCKMODE_UPDATE);
    }

    @Override
    public final void ensure(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"deferredEnsure");
        }

        ensure((BetaTransaction)tx);
    }

    @Override
    public final void ensure(final Transaction tx){
        ensure((BetaTransaction)tx);
    }

    public final void ensure(final BetaTransaction tx){
        if(tx == null){
            throw new NullPointerException();
        }

        tx.materializeConflict(this);
    }

    @Override
    public final void acquireCommitLock(){
        Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"privatize");
        }

        acquireCommitLock((BetaTransaction)tx);
    }

    @Override
    public final void acquireCommitLock(Transaction tx){
        acquireCommitLock((BetaTransaction)tx);
    }

    public final void acquireCommitLock(BetaTransaction tx){
        tx.openForRead(this, LOCKMODE_COMMIT);
    }

    @Override
    public final void commute(IntFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"commute");
        }

        commute((BetaTransaction)tx, function);
    }

    @Override
    public final void commute(final Transaction tx, final IntFunction function){
        commute((BetaTransaction)tx, function);
    }

    public final void commute(BetaTransaction tx,IntFunction function){
        tx.commute(this, function);
    }

    @Override
    public final int atomicAlterAndGet(final IntFunction function){
        return atomicAlter(function, false);
    }

    @Override
    public final int alterAndGet(final IntFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"alterAndGet");
        }

        return alterAndGet((BetaTransaction)tx, function);
    }

    @Override
    public final int alterAndGet(final Transaction tx,final IntFunction function){
        return alterAndGet((BetaTransaction)tx, function);
    }

    public final int alterAndGet(final BetaTransaction tx,final IntFunction function){
        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        BetaIntRefTranlocal write
            = (BetaIntRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

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
    public final int atomicGetAndAlter(final IntFunction function){

        return atomicAlter(function,true);
    }

    private int atomicAlter(final IntFunction function,final boolean returnOld){
        if(function == null){
            throw new NullPointerException("Function can't be null");
        }

        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final int oldValue = ___value;
        int newValue;
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

        ___departAfterUpdateAndUnlock();

        if(listeners!=null){
           listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return returnOld ? oldValue : newValue;
    }

    @Override
    public final int getAndAlter(final IntFunction function){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndAlter");
        }

        return getAndAlter((BetaTransaction)tx, function);
    }

    @Override
    public final int getAndAlter(final Transaction tx,final IntFunction function){
        return getAndAlter((BetaTransaction)tx, function);
    }

    public final int getAndAlter(final BetaTransaction tx,final IntFunction function){
        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        BetaIntRefTranlocal write
            = (BetaIntRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

        final int oldValue = write.value;
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
    public final boolean atomicCompareAndSet(final int expectedValue,final int newValue){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final int currentValue = ___value;

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

        ___departAfterUpdateAndUnlock();

        if(listeners!=null){
            listeners.openAll(getThreadLocalBetaObjectPool());
        }

        return true;
    }

    @Override
    public final int getAndSet(final int value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"getAndSet");
        }

        return getAndSet((BetaTransaction)tx, value);
    }

    public final int set(final int value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"set");
        }

        return set((BetaTransaction)tx, value);
    }

    @Override
    public final int get(){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"get");
        }

        return get((BetaTransaction)tx);
    }

    @Override
    public final int get(final Transaction tx){
        return get((BetaTransaction)tx);
    }

    public final int get(final BetaTransaction transaction){
        return transaction.openForRead(this, LOCKMODE_NONE).value;
    }

    @Override
    public final int atomicGet(){
        int attempt = 1;
        do{
            if(!___hasCommitLock()){

                int read = ___value;

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
    public final int atomicWeakGet(){
       return ___value;
    }

    @Override
    public final int atomicSet(final int newValue){
        atomicGetAndSet(newValue);
        return newValue;
    }

    @Override
    public final int atomicGetAndSet(final int newValue){
        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final int oldValue = ___value;

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

        ___departAfterUpdateAndUnlock();

        if(listeners != null){
            BetaObjectPool pool = getThreadLocalBetaObjectPool();
            listeners.openAll(pool);
        }

        return oldValue;
    }

    @Override
    public final int set(Transaction tx, int value){
        return set((BetaTransaction)tx, value);
    }

    public final int set(final BetaTransaction tx,final int value){
        tx.openForWrite(this, LOCKMODE_NONE).value = value;
        return value;
    }

    @Override
    public final int getAndSet(final Transaction tx,final int value){
        return getAndSet((BetaTransaction)tx, value);
    }

    public final int getAndSet(final BetaTransaction tx,final int value){
        BetaIntRefTranlocal write = tx.openForWrite(this, LOCKMODE_NONE);
        int oldValue = write.value;
        write.value = value;
        return oldValue;
    }

    @Override
    public final void await(int value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"await");
        }

        await((BetaTransaction)tx, value);
    }

    @Override
    public final void await(final Transaction tx,final int value){
        await((BetaTransaction)tx, value);
    }

    public final void await(final BetaTransaction tx,final int value){
        BetaIntRefTranlocal read = tx.openForRead(this,LOCKMODE_NONE);
        if(read.value != value){
            tx.retry();
        }
    }

    @Override
    public final void await(final IntPredicate predicate){
        final Transaction tx = getThreadLocalTransaction();

        if(tx == null){
            throw new TransactionRequiredException(getClass(),"await");
        }

        await((BetaTransaction)tx, predicate);
    }

    @Override
    public final void await(final Transaction tx, IntPredicate predicate){
        await((BetaTransaction)tx, predicate);
    }

    public final void await(final BetaTransaction tx, IntPredicate predicate){
        if(tx == null){
            throw new NullPointerException();
        }

        if(predicate == null){
            tx.abort();
            throw new NullPointerException();
        }

        boolean abort = true;
        try{
            final int value = tx.openForRead(this, LOCKMODE_NONE).value;
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
