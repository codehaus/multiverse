package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.StmUtils;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.AtomicOperationException;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.predicates.IntPredicate;
import org.multiverse.api.references.IntRef;
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
public final class BetaIntRef
    extends VeryAbstractBetaTransactionalObject
    implements IntRef
{

    //Active needs to be volatile. If not, the both load statements in the load function, can be reordered
    //(the instruction above can jump below the orec.arrive if no write is done)
    private volatile IntRefTranlocal ___active;

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
        IntRefTranlocal tranlocal =
            new IntRefTranlocal(this);

        tranlocal.value = initialValue;
        tranlocal.isCommitted = true;
        tranlocal.isDirty = DIRTY_FALSE;
        ___active = tranlocal;
    }

     @Override
    public final int ___getClassIndex(){
        return 1;
    }

   @Override
    public final IntRefTranlocal ___load(
        final int spinCount,
        final BetaTransaction newLockOwner,
        final int lockMode) {
        //it can't happen that the isPermanent status of a tranlocal is changed while it is being used. This is
        //because an arrive is done, and as long as there is at least 1 arive, the orec never can become readbiased.

        if(lockMode == LOCKMODE_NONE){
            while (true) {
                //JMM: nothing can jump behind the following statement
                IntRefTranlocal read = ___active;

                //JMM: the read for the arrive can't jump over the read of the active.
                final int arriveStatus = ___arrive(spinCount);

                if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                    return IntRefTranlocal.LOCKED;
                }

                //as long as there are readers (done after the arrive), the read tranlocal can't be pooled.
                //So after the arrive is done, we don't need to worry about the tranlocal to re-appear as reused
                //tranlocal. This means that the read/arrive/read mechanism doesn't cause problems with pooling.

                //JMM safety:
                //The volatile read of active can't be reordered so that it jump in front of the volatile read of
                //the orec-value when the arrive method is called.
                //An instruction is allowed to jump in front of the write of orec-value, but it is not allowed to
                //jump in front of the read or orec-value (volatile read happens before rule).
                //This means that it isn't possible that a locked value illegally is seen as unlocked.

                if (___active == read) {
                    //at this point we are sure that the read was unlocked.

                    if(arriveStatus == ARRIVE_UNREGISTERED){
                        read.isPermanent = true;
                    }
                    return read;
                }

                //we are not lucky, the value has changed. But before retrying, we need to depart if the arrive was
                //not permanent.
                if (read != null && arriveStatus == ARRIVE_NORMAL) {
                    ___departAfterFailure();
                }
            }
        }else{
            final boolean commitLock = lockMode == LOCKMODE_COMMIT;

            if(newLockOwner == null){
                throw new NullPointerException();
            }

            //JMM: no instructions will jump in front of a volatile read. So this stays on top.
            final int arriveStatus = ___tryLockAndArrive(___stm.spinCount, commitLock);
            if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
                return  IntRefTranlocal.LOCKED;
            }

            ___lockOwner = newLockOwner;

            IntRefTranlocal read = ___active;
            if(arriveStatus == ARRIVE_UNREGISTERED){
                read.isPermanent = true;
            }

            return read;
        }
    }

    @Override
    public final IntRefTranlocal ___unsafeLoad() {
        return ___active;
    }

    @Override
    public final boolean ___hasReadConflict(
        final Tranlocal tranlocal,
        final BetaTransaction tx) {

        //if the current transaction owns the lock, there is no conflict...
        //todo: only going to work when the acquire lock also does a conflict check.
        if(___lockOwner == tx){
            return false;
        }

        final Tranlocal read = tranlocal.isCommitted ? tranlocal: tranlocal.read;

        //if the active value is different, we are certain of a conflict
        if(___active != read){
            return true;
        }

        //another transaction currently has the lock, and chances are that the transaction
        //is going to update the value. We can't assume that even though the current active value
        //is still the same, that the transaction isn't going to overwrite it and cause a read conflict.
        return ___hasCommitLock();
    }

    @Override
    public final boolean ___tryLockAndCheckConflict(
        final BetaTransaction newLockOwner,
        final int spinCount,
        final Tranlocal tranlocal,
        final boolean commitLock) {

        //If it already is locked by the current transaction, we are done.
        //Fresh constructed objects always have the tx set.
        if (___lockOwner == newLockOwner) {
            if(commitLock){
                ___upgradeToCommitLock();
            }
            return true;
        }

        final Tranlocal read = tranlocal.isCommitted ? tranlocal : tranlocal.read;
        if(read.isPermanent){
            //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
            final int arriveStatus = ___tryLockAndArrive(spinCount, commitLock);
            if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
                return false;
            }

            //we have successfully acquired the lock
            ___lockOwner = newLockOwner;
            return read == ___active;
        }

        if (!___tryLockAfterNormalArrive(spinCount, commitLock)) {
            return false;
        }

        //we have successfully acquired the lock
        ___lockOwner = newLockOwner;
        return read == ___active;
    }

    @Override
    public final IntRefTranlocal ___openForConstruction(
        final BetaObjectPool pool) {
        IntRefTranlocal tranlocal =  pool.take(this);
        return tranlocal != null ? tranlocal : new IntRefTranlocal(this);
    }

    @Override
    public final IntRefTranlocal ___openForCommute(
        final BetaObjectPool pool) {

        IntRefTranlocal tranlocal =  pool.take(this);

        if(tranlocal == null){
             tranlocal = new IntRefTranlocal(this);
        }

        tranlocal.isCommuting = true;
        return tranlocal;
    }

    @Override
    public final Listeners ___commitDirty(
            final Tranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        final boolean notDirty = tranlocal.isDirty == DIRTY_FALSE;

        if(notDirty){
            final boolean ownsLock = expectedLockOwner == ___lockOwner;
            final IntRefTranlocal read = (IntRefTranlocal)(tranlocal.isCommitted
                ?tranlocal
                :tranlocal.read);

            if(ownsLock){
                ___lockOwner = null;

                if(read.isPermanent){
                    ___unlockByReadBiased();
                }else{
                    ___departAfterReadingAndUnlock();
                }
            }else{
                if(!read.isPermanent){
                    ___departAfterReading();
                }
            }

            return null;
        }

        ___lockOwner = null;

        //it is a full blown update (so locked).
        final IntRefTranlocal newActive = (IntRefTranlocal)tranlocal;
        newActive.prepareForCommit();
        final IntRefTranlocal oldActive = ___active;
        ___active = newActive;

        //JMM: problem, it could happen that volatile read this.listeners jumps in front of the volatile write
        //to this.active. This means that we could break the contract we have: an update is responsible for all
        //listeners that have registered before the actual write happens. So it needs to remove the listeners
        //after the actual write happens.

        //it is important that this call is done after the actual write. This is needed to give the guarantee
        //that we are going to take care of all listeners that are registered before that write. The read is done
        //after the unlock because that requires a volatile read. It could be that a listener is notified that
        //already has registered for the following read, so there could be a spurious wakeup. This was done
        //to prevent a JMM problem where the read of the listeners, could jump in front of the write to the value
        //and it is not possible that the read jump over the release of the lock.
             
        Listeners listenersAfterWrite = null;

        if(___listeners != null){
            //at this point it could have happened that the listener has changed.. it could also

            while(true){
                listenersAfterWrite = ___listeners;
                if(___unsafe.compareAndSwapObject(this, listenersOffset, listenersAfterWrite, null)){
                    break;
                }
            }
        }

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0 && oldActive!=null) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        return listenersAfterWrite;
    }

    private Listeners ___removeListenersAfterWrite(){
        if(___listeners == null){
            return null;
        }

        //at this point it could have happened that the listener has changed.. it could also
        Listeners result;
        while(true){
            result = ___listeners;
            if(___unsafe.compareAndSwapObject(this, listenersOffset, result, null)){
                return result;
            }
        }
    }

    @Override
    public final Listeners ___commitAll(
            final Tranlocal tranlocal,
            final BetaTransaction expectedLockOwner,
            final BetaObjectPool pool) {

        if(expectedLockOwner != ___lockOwner){
            //it can't be an update, otherwise the lock would have been acquired.

            if(!tranlocal.isPermanent){
                ___departAfterReading();
            }
            return null;
        }

        ___lockOwner = null;

        if(tranlocal.isCommitted){
            if(tranlocal.isPermanent){
                ___unlockByReadBiased();
            }else{
                ___departAfterReadingAndUnlock();
            }
            return null;
        }

        //it is a full blown update (so locked).
        final IntRefTranlocal newActive = (IntRefTranlocal)tranlocal;
        newActive.prepareForCommit();
        final IntRefTranlocal oldActive = ___active;

        ___active = newActive;

        //we don't need to worry about other updates since it is locked, but we do need to worry about threads
        //that register themselves for change. One of the biggest problems that can happen is that we forget to
        //wake up a a transaction we should have woken up. Waking them up too too early (a spurious wakeup) is
        //less harmful since the transaction will be retried.

        //it is important that this call is done after the actual write. This is needed to give the guarantee
        //that we are going to take care of all listeners that are registered before that write.
        //The registerRetryListener takes care of that situation.

        Listeners listenersAfterWrite = ___listeners;

        if(listenersAfterWrite != null){
            //at this point it could have happened that the listener has changed.. it could also

            if(!___unsafe.compareAndSwapObject(this, listenersOffset, listenersAfterWrite, null)){
                listenersAfterWrite = null;
            }
        }

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0 && oldActive != null) {
            //nobody is using the tranlocal anymore, so pool it.

            //todo: permanent tranlocals also are pooled, but could this cause problems with less
            //strict reads
            pool.put(oldActive);
        }

        return listenersAfterWrite;
    }

    @Override
    public final void ___abort(
        final BetaTransaction transaction,
        final Tranlocal tranlocal,
        final BetaObjectPool pool) {

        IntRefTranlocal read;
        if (tranlocal.isCommitted) {
            read = (IntRefTranlocal)tranlocal;
        } else {
            read = (IntRefTranlocal)tranlocal.read;
             //if there is an update, it can always be pooled since it is impossible that it has been
            //read by another transaction.
            pool.put((IntRefTranlocal)tranlocal);

            //if it is a constructed object, we don't need to abort. Constructed objects from aborted transactions,
            //should remain locked indefinitely since their behavior is undefined.
            if(read == null){
                return;
            }
        }

        if (___lockOwner != transaction) {
            //the current transaction didn't own the lock.
            if (!read.isPermanent) {
                //it is important that the depart is not called when the read isReadBiased. It could
                //be that the orec already has become updateBiased, and if we call a depart, we are
                //departing too much.
                ___departAfterFailure();
            }

            return;
        }

        //the current transaction owns the lock.. so lets release it
        ___lockOwner = null;

        //depart and release the lock. This call is able to deal with readbiased and normal reads.
        ___departAfterFailureAndUnlock();
    }

    @Override
    public final int ___registerChangeListener(
        final Latch latch,
        final Tranlocal tranlocal,
        final BetaObjectPool pool,
        final long listenerEra){

        if(tranlocal.isCommuting){
            return REGISTRATION_NONE;
        }

        final Tranlocal read = tranlocal.isCommitted ? tranlocal:tranlocal.read;

        if(read == null){
            //it is a constructed one, and we can't register for change on it.
            return REGISTRATION_NONE;
        }

        //if it currently contains a different active tranlocal, we don't need to wait at all.
        if(read != ___active){
            latch.open(listenerEra);
            return REGISTRATION_NOT_NEEDED;
        }

        //we are going to register the listener since the current value still matches with is active.
        //But it could be that the registration completes after the write has happened.

        Listeners newListeners = pool.takeListeners();
        if(newListeners == null){
            newListeners = new Listeners();
        }
        newListeners.listener = latch;
        newListeners.listenerEra = listenerEra;

        //we need to do this in a loop because other register thread could be contending for the same
        //listeners field.
        while(true){
            //the listeners object is mutable, but as long as it isn't yet registered, this calling
            //thread has full ownership of it.
            final Listeners oldListeners = ___listeners;

            newListeners.next = oldListeners;

            //lets try to register our listeners.
            if(!___unsafe.compareAndSwapObject(this, listenersOffset, oldListeners, newListeners)){
                //so we are contending with another register thread, so lets try it again. Since the compareAndSwap
                //didn't succeed, we know that the current thread still has exclusive ownership on the Listeners object.
                continue;
            }

            //the registration was a success. We need to make sure that the active hasn't changed.
            //JMM: the volatile read can't jump in front of the unsafe.compareAndSwap.
            if(read == ___active){
                //we are lucky, the registration was done successfully and we managed to cas the listener
                //before the update (since the update hasn't happened yet). This means that the updating thread
                //is now responsible for notifying the listeners.
                return REGISTRATION_DONE;
            }

            //JMM: the unsafe.compareAndSwap can't jump over the volatile read this.active.
            //the update has taken place, we need to check if our listeners still is in place.
            //if it is, it should be removed and the listeners notified. If the listeners already has changed,
            //it is the task for the other to do the listener cleanup and notify them
            if(___unsafe.compareAndSwapObject(this, listenersOffset, newListeners, null)){
                newListeners.openAll(pool);
            }else{
                latch.open(listenerEra);
            }

            return REGISTRATION_NOT_NEEDED;
        }
    }

    @Override
    public void addDeferredValidator(IntPredicate validator){
        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            addDeferredValidator((BetaTransaction)tx, validator);
            return;
        }

        atomicAddDeferredValidator(validator);
    }

    @Override
    public void addDeferredValidator(Transaction tx, IntPredicate validator){
        addDeferredValidator((BetaTransaction)tx, validator);
    }

    public void addDeferredValidator(BetaTransaction tx, IntPredicate validator){
        if(tx == null){
            throw new NullPointerException();
        }

        if(validator == null){
            tx.abort();
            throw new NullPointerException();
        }

        IntRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);
        if(write.validators == null){
            write.validators = new IntPredicate[1];
            write.validators[0]=validator;
        }else{
            throw new TodoException();
        }        
    }

    @Override
    public void atomicAddDeferredValidator(IntPredicate validator){
        if(validator == null){
            throw new NullPointerException();
        }
        throw new TodoException();
    }
    
    @Override
    public final int atomicGetAndIncrement(final int amount){
        int result = atomicIncrementAndGet(amount);
        return result - amount;
    }

    @Override
    public final int getAndIncrement(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return getAndIncrement((BetaTransaction)tx, amount);
        }

        return atomicGetAndIncrement(amount);
    }

    @Override
    public final int getAndIncrement(final Transaction tx, final int amount){
        return getAndIncrement((BetaTransaction)tx, amount);
    }

    public final int getAndIncrement(final BetaTransaction tx, final int amount){
        IntRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

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

        final IntRefTranlocal oldActive = ___active;
        final int oldValue = oldActive.value;

        if(amount == 0){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return oldValue;
        }

       final BetaObjectPool pool = getThreadLocalBetaObjectPool();
        IntRefTranlocal update = pool.take(this);
        if(update == null){
            update = new IntRefTranlocal(this);
        }

        final int newValue = oldValue + amount;
        update.value  = newValue;
        update.prepareForCommit();
        ___active = update;

        Listeners listeners = ___removeListenersAfterWrite();

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        if(listeners!=null){
            listeners.openAll(pool);
        }

        return newValue; 
    }

    @Override
    public final int incrementAndGet(final int amount){
        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return incrementAndGet((BetaTransaction)tx, amount);
        }

        return atomicIncrementAndGet(amount);
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

        IntRefTranlocal write= tx.openForWrite(this, LOCKMODE_NONE);

        write.value+=amount;
        return write.value;
    }

    @Override
    public final void ensure(){
        Transaction tx = getThreadLocalTransaction();

        if(tx!=null && tx.isAlive()){
            ensure((BetaTransaction)tx);
            return;
        }

        throw new NoTransactionFoundException("No transaction is found for the ensure operation");
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

        if(tx!=null && tx.isAlive()){
            return tryEnsure((BetaTransaction)tx);            
        }

        throw new NoTransactionFoundException("No transaction is found for the tryEnsure operation");
    }

    @Override
    public final boolean tryEnsure(final Transaction tx){
        return tryEnsure((BetaTransaction)tx);
    }

    public final boolean tryEnsure(BetaTransaction tx){
        return tx.tryLock(this, LOCKMODE_UPDATE);
    }

    @Override
    public final void ensureOptimistic(){
        throw new TodoException();
    }

    @Override
    public final void ensureOptimistic(final Transaction tx){
        ensureOptimistic((BetaTransaction)tx);
    }

    public final void ensureOptimistic(final BetaTransaction tx){
        throw new TodoException();
    }

    @Override
    public final void privatize(){
        Transaction tx = getThreadLocalTransaction();

        if(tx!=null && tx.isAlive()){
            privatize((BetaTransaction)tx);
            return;
        }

        throw new NoTransactionFoundException("No transaction is found for the privatize operation");
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

        if(tx!=null && tx.isAlive()){
            return tryPrivatize((BetaTransaction)tx);
        }

        throw new NoTransactionFoundException("No transaction is found for the tryPrivatize operation");
    }

    @Override
    public final boolean tryPrivatize(Transaction tx){
        return tryPrivatize((BetaTransaction)tx);
    }

    public final boolean tryPrivatize(BetaTransaction tx){
        return tx.tryLock(this, LOCKMODE_COMMIT);
    }

    @Override
    public final void commute(
        IntFunction function){

        final Transaction tx = getThreadLocalTransaction();

        if(tx!=null && tx.isAlive()){
            commute((BetaTransaction)tx, function);
            return;
        }

        atomicAlter(function, false);
    }

    @Override
    public final void commute(
        final Transaction tx,
        final IntFunction function){

        commute((BetaTransaction)tx, function);
    }

    public final void commute(
        BetaTransaction tx,
        IntFunction function){

        tx.commute(this, function);
    }

    @Override
    public final int atomicAlterAndGet(
        final IntFunction function){

        return atomicAlter(function, false);
    }

    @Override
    public final int alterAndGet(
        final IntFunction function){

        final Transaction tx = getThreadLocalTransaction();

        if(tx!=null && tx.isAlive()){
            return alterAndGet((BetaTransaction)tx, function);
        }

        return atomicAlterAndGet(function);
    }

    @Override
    public final int alterAndGet(
        final Transaction tx,
        final IntFunction function){
        return alterAndGet((BetaTransaction)tx, function);
    }

    public final int alterAndGet(
        final BetaTransaction tx,
        final IntFunction function){

        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        IntRefTranlocal write
            = (IntRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

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
    public final int atomicGetAndAlter(
        final IntFunction function){

        return atomicAlter(function,true);
    }

    private int atomicAlter(
        final IntFunction function,
        final boolean returnOld){

        if(function == null){
            throw new NullPointerException("Function can't be null");
        }

        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final IntRefTranlocal oldActive = ___active;

        final int oldValue = oldActive.value;
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

        final BetaObjectPool pool = getThreadLocalBetaObjectPool();
        IntRefTranlocal update = pool.take(this);
        if(update == null){
            update = new IntRefTranlocal(this);
        }

        update.value = newValue;
        update.prepareForCommit();
        ___active = update;

        Listeners listeners = ___removeListenersAfterWrite();

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        if(listeners!=null){
            listeners.openAll(pool);
        }

        return returnOld ? oldValue : newValue;
    }

    @Override
    public final int getAndAlter(
        final IntFunction function){

        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return getAndAlter((BetaTransaction)tx, function);
        }

        return atomicGetAndAlter(function);
    }

    @Override
    public final int getAndAlter(
        final Transaction tx,
        final IntFunction function){

        return getAndAlter((BetaTransaction)tx, function);
    }

    public final int getAndAlter(
        final BetaTransaction tx,
        final IntFunction function){

        if(function == null){
            tx.abort();
            throw new NullPointerException("Function can't be null");
        }

        IntRefTranlocal write
            = (IntRefTranlocal)tx.openForWrite(this, LOCKMODE_NONE);

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
    public final boolean atomicCompareAndSet(
        final int expectedValue,
        final int newValue){

        final int arriveStatus = ___arriveAndLockOrBackoff();

        if(arriveStatus == ARRIVE_LOCK_NOT_FREE){
            throw new LockedException();
        }

        final IntRefTranlocal oldActive = ___active;

        if(oldActive.value != expectedValue){
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

        //lets create a tranlocal for the update.
        final BetaObjectPool pool = getThreadLocalBetaObjectPool();
        IntRefTranlocal update = pool.take(this);
        if(update == null){
            update = new IntRefTranlocal(this);
        }

        update.value = newValue;
        update.prepareForCommit();
        ___active = update;

        Listeners listeners = ___removeListenersAfterWrite();

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        if(listeners!=null){
            listeners.openAll(pool);
        }

        return true;
    }

    @Override
    public final int getAndSet(final int value){

        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return getAndSet((BetaTransaction)tx, value);
        }

        return atomicGetAndSet(value);
    }

    public final int set(final int value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return set((BetaTransaction)tx, value);
        }

        return atomicSet(value);
    }

    @Override
    public final int get(){
        final Transaction tx = getThreadLocalTransaction();

        if(tx != null && tx.isAlive()){
            return get((BetaTransaction)tx);
        }

        return atomicGet();
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
        IntRefTranlocal read = ___load(___stm.spinCount, null, LOCKMODE_NONE);

        if(read == null){
            throw new AtomicOperationException();
        }

        if(read.isLocked){
            throw new LockedException("Can't read locked reference");
        }

        int result = read.value;

        if(!read.isPermanent){
            ___departAfterReading();
        }

        return result;
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

        final IntRefTranlocal oldActive = ___active;
        final int oldValue = oldActive.value;
        if(oldValue == newValue){
            if(arriveStatus == ARRIVE_UNREGISTERED){
                ___unlockByReadBiased();
            } else{
                ___departAfterReadingAndUnlock();
            }

            return newValue;
        }

        //lets create a tranlocal for the update.
        final BetaObjectPool pool = getThreadLocalBetaObjectPool();               
        IntRefTranlocal update = pool.take(this);
        if(update == null){
            update = new IntRefTranlocal(this);
        }

        //lets do the update.
        update.value = newValue;
        update.prepareForCommit();
        ___active = update;

        Listeners listeners = ___removeListenersAfterWrite();

        long remainingSurplus = ___departAfterUpdateAndUnlock(___stm.globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        if(listeners!=null){
            listeners.openAll(pool);
        }

        return oldValue;
    }

    @Override
    public final int set(Transaction tx, int value){
        return set((BetaTransaction)tx, value);
    }

    public final int set(
        final BetaTransaction tx,
        final int value){

        tx.openForWrite(this, LOCKMODE_NONE).value = value;
        return value;
    }

    @Override
    public final int getAndSet(
        final Transaction tx,
        final int value){

        return getAndSet((BetaTransaction)tx, value);
    }

    public final int getAndSet(
        final BetaTransaction tx,
        final int value){

        IntRefTranlocal write = tx.openForWrite(this, LOCKMODE_NONE);
        int oldValue = write.value;
        write.value = value;
        return oldValue;
    }

    @Override
    public final void await(int value){
        final Transaction tx = getThreadLocalTransaction();

        if(tx!=null && tx.isAlive()){
            await((BetaTransaction)tx, value);
            return;
        }

        throw new TodoException();
    }

    @Override
    public final void await(
        final Transaction tx,
        final int value){

        await((BetaTransaction)tx, value);
    }

    public final void await(
        final BetaTransaction tx,
        final int value){

        IntRefTranlocal read = tx.openForRead(this,LOCKMODE_NONE);
        if(read.value != value){
            StmUtils.retry();
        }        
    }
}
