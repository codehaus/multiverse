package org.multiverse.stms.beta.refs;

import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
public final class LongRef extends FastOrec implements BetaTransactionalObject {
    public final static AtomicLong pooled = new AtomicLong();
    public final static AtomicLong nonPooled = new AtomicLong();
    public final static AtomicInteger classIndexGenerator = new AtomicInteger();

    private final static long listenersOffset;

    static {
        try {
            listenersOffset = unsafe.objectFieldOffset(LongRef.class.getDeclaredField("listeners"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public BetaTransaction lockOwner;

    //Active needs to be volatile. If not, the both load statements in the load function, can be reordered
    //(the instruction above can jump below the orec.arrive if no write is done)
    public volatile LongRefTranlocal active;

    private volatile Listeners listeners;
    private String storageId;

    //controlled JMM problem (just like the hashcode of String).
    private int identityHashCode;

    public LongRef(BetaTransaction tx){
        arriveAndLockForUpdate(0);
        this.lockOwner = tx;
    }

    //todo: this constructor needs to be removed.
    public LongRef(){
    }

    public LongRef(long value){
        active = new LongRefTranlocal(this);
        active.value = value;
        active.isCommitted = true;
    }

    @Override
    public final BetaTransaction getLockOwner() {
        return lockOwner;
    }

    @Override
    public final int getClassIndex(){
        return 2;
    }

    @Override
    public final Orec getOrec() {
        return this;
    }

    @Override
    public final LongRefTranlocal load(final int spinCount) {
        while (true) {
            LongRefTranlocal loaded = active;

            //will also work as a read-barrier.
            if (!arrive(spinCount)) {
                return LongRefTranlocal.LOCKED;
            }

            if (active == loaded) {
                //at this point we are sure that the tranlocal we have read is unlocked.
                return loaded;
            }

            //we are not lucky, the value has changed. But before retrying, we need to depart
            //if needed.
            if (loaded != null && !loaded.isPermanent()) {
                departAfterFailure();
            }
        }
    }

    @Override
    public final LongRefTranlocal lockAndLoad(final int spinCount, final BetaTransaction newLockOwner) {
        assert newLockOwner != null;

        if (lockOwner == newLockOwner) {
            return active;
        }

        if (!arriveAndLockForUpdate(spinCount)) {
            return  LongRefTranlocal.LOCKED;
        }

        lockOwner = newLockOwner;
        return active;
    }

    @Override
    public final LongRefTranlocal unsafeLoad() {
        return active;
    }

    @Override
    public final LongRefTranlocal openForConstruction(final ObjectPool pool) {
        LongRefTranlocal tranlocal =  pool.take(this);
        return tranlocal != null ? tranlocal : new LongRefTranlocal(this);
    }

    @Override
    public void commitConstructed(final Tranlocal tranlocal){
        final LongRefTranlocal newActive = (LongRefTranlocal)tranlocal;
        newActive.prepareForCommit();
        active = newActive;
    }

    @Override
    public final Listeners commitDirty(
            final Tranlocal tranlocal, final BetaTransaction expectedLockOwner, final ObjectPool pool,
            final GlobalConflictCounter globalConflictCounter) {

        if(expectedLockOwner != lockOwner){
            Tranlocal read = tranlocal.isCommitted?tranlocal:tranlocal.read;

            //it can't be an update, otherwise the lock would have been acquired.
            if(!read.isPermanent && departAfterReading()){
                //Only a non parmenent tranlocal is allowed to do a depart.
                //The orec indicates that it has become time to transform the tranlocal to mark as permanent.

                ((LongRefTranlocal)read).markAsPermanent();
                unlockAfterBecomingReadBiased();
            }
            return null;
        }

        lockOwner = null;

        if(!tranlocal.isDirty){
            Tranlocal read = tranlocal.isCommitted?tranlocal:tranlocal.read;

            if(read.isPermanent){
                unlockByPermanent();
            }else{
                if(departAfterReadingAndReleaseLock()){
                    //the orec indicates that it has become time to transform the tranlocal to mark as permanent

                    ((LongRefTranlocal)read).markAsPermanent();
                    unlockAfterBecomingReadBiased();
                }
            }
            return null;
        }

        //listeners need to be read before the lock is released, to prevent waking up listeners, that
        //already have registered for next generations of listeners.

        //it is a full blown update (so locked).
        final LongRefTranlocal newActive = (LongRefTranlocal)tranlocal;
        newActive.prepareForCommit();
        final LongRefTranlocal oldActive = active;
        active = newActive;

        //it is important that this call is done after the actual write. This is needed to give the guarantee
        //that we are going to take care of all listeners that are registered before that write.
        Listeners listenersAfterWrite = listeners;

        long remainingSurplus = departAfterUpdateAndReleaseLock(globalConflictCounter, this);

        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        if(listenersAfterWrite != null){
            if(unsafe.compareAndSwapObject(this, listenersOffset, listenersAfterWrite, null)){
                return listenersAfterWrite;
            }
        }

        return null;
    }

    @Override
    public final Listeners commitAll(
            final Tranlocal tranlocal, final BetaTransaction expectedLockOwner, final ObjectPool pool,
            final GlobalConflictCounter globalConflictCounter) {

        if(expectedLockOwner != lockOwner){
            //it can't be an update, otherwise the lock would have been acquired.

            if(!tranlocal.isPermanent && departAfterReading()){
                //Only a non parmenent tranlocal is allowed to do a depart.
                //The orec indicates that it has become time to transform the tranlocal to mark as permanent.

                ((LongRefTranlocal)tranlocal).markAsPermanent();
                unlockAfterBecomingReadBiased();
            }
            return null;
        }

        lockOwner = null;

        if(tranlocal.isCommitted){
            if(tranlocal.isPermanent){
                unlockByPermanent();
            }else{
                if(departAfterReadingAndReleaseLock()){
                    //the orec indicates that it has become time to transform the tranlocal to mark as permanent

                    ((LongRefTranlocal)tranlocal).markAsPermanent();
                    unlockAfterBecomingReadBiased();
                }
            }
            return null;
        }

        //it is a full blown update (so locked).
        final LongRefTranlocal newActive = (LongRefTranlocal)tranlocal;
        newActive.prepareForCommit();
        final LongRefTranlocal oldActive = active;

        active = newActive;

        //we don't need to worry about other updates since it is locked, but we do need to worry about threads
        //that register themselves for change. One of the biggest problems that can happen is that we forget to
        //wake up a a transaction we should have woken up. Wakeing them up too too early (a spurious wakeup) is
        //less harmfull since the transaction will be retried.

        //it is important that this call is done after the actual write. This is needed to give the guarantee
        //that we are going to take care of all listeners that are registered before that write.
        //The registerRetryListener takes care of that situation.
        Listeners listenersAfterWrite = listeners;

        long remainingSurplus = departAfterUpdateAndReleaseLock(globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        //todo: it could be that listeners are picked up that already rely on a newer update.
        //so you could have spurious wakups here. It would be better to read the listeners
        //before the release of the lock... if that value is not null,
        if(listenersAfterWrite != null){
            if(unsafe.compareAndSwapObject(this, listenersOffset, listenersAfterWrite, null)){
                //the current thread has now full ownership of the listeners chain. So it can also put them
                //back in the pool.
                return listenersAfterWrite;
            }
        }

        return null;
    }

    @Override
    public final boolean hasReadConflict(final Tranlocal tranlocal, final BetaTransaction tx) {
        final boolean committed = tranlocal.isCommitted();

        Tranlocal read = committed ? tranlocal: tranlocal.read;

        //if the active value is different, we are certain of a conflict
        if(active != read){
            return true;
        }

        //if the current transaction owns the lock, there is no conflict...
        //todo: only going to work when the acquire lock also does a conflict check.
        if(lockOwner == tx){
            return false;
        }

        //there is never a conflict on a fresh object.
        if(committed && read==null){
            return false;
        }

        //another transaction currently has the lock, and chances are that the transaction
        //is going to update the value. We can't asume that even though the current active value
        //is still the same, that the transaction isn't going to overwrite it and cause a read conflict.
        return isLocked();
    }

    @Override
    public boolean tryLockAndCheckConflict(final BetaTransaction newLockOwner, final int spinCount, final Tranlocal tranlocal) {
        //if it already is locked by the current transaction, we are done.
        if (lockOwner == newLockOwner) {
            return true;
        }

        //if the lock can't be acquired, we are done and can return false.
        if (!tryUpdateLock(spinCount)) {
            return false;
        }

        //we have successfully acquired the lock
        lockOwner = newLockOwner;

        Tranlocal read = tranlocal.isCommitted ? tranlocal : tranlocal.read;
        if(read != active){
            return false;
        }

        return true;
    }

    @Override
    public final void abort(final BetaTransaction transaction, final Tranlocal tranlocal, final ObjectPool pool) {
        assert transaction != null;
        assert tranlocal != null;
        assert pool!=null;

        LongRefTranlocal update;
        LongRefTranlocal read;
        if (tranlocal.isCommitted) {
            update = null;
            read = (LongRefTranlocal)tranlocal;
        } else {
            update = (LongRefTranlocal)tranlocal;
            read = (LongRefTranlocal)tranlocal.read;
        }

        if (update != null) {
            //if there is an update, it can always be pooled since it is impossible that it has been
            //read by another transaction.
            pool.put(update);
        }

        //if it is a constructed object, we don't need to abort. Constructed objects from aborted transactions,
        //should remain locked indefinitly since their behavior is undefined.
        if(read == null){
            return;
        }

        if (lockOwner != transaction) {
            //the current transaction didn't own the lock.
            if (read != null && !read.isPermanent) {
                //it is important that the depart is not called when the read isReadBiased. It could
                //be that the orec already has become updateBiased, and if we call a depart, we are
                //departing too much.
                departAfterFailure();
            }

            return;
        }

        //the current transaction owns the lock.. so lets release it
        lockOwner = null;
        departAfterFailureAndReleaseLock();
    }

    @Override
    public boolean registerChangeListener(
        final Latch latch, final Tranlocal tranlocal, final ObjectPool pool, final long listenerEra){

        Tranlocal read = tranlocal.isCommitted?tranlocal:tranlocal.read;

        //if it currently contains a different active tranlocal, we don't need to wait at all.
        if(read != active){
            latch.open(listenerEra);
            return false;
        }

        //we are going to register the listener since the current value still matches with is active.
        //But it could be that the registration completes after the

        //lets create us an update
        Listeners update = pool.takeListeners();
        if(update == null){
            update = new Listeners();
        }

        //we need to do this in a loop because other register thread could be contending for the same
        //listeners field.

        while(true){
            //the listeners object is mutable, but as long as it isn't yet registered, this calling
            //thread has full ownership of it.
            Listeners expected = listeners;
            update.next = expected;
            update.listener = latch;
            update.listenerEra = listenerEra;

            //lets try to register our listeners.
            if(unsafe.compareAndSwapObject(this, listenersOffset, expected, update)){
                //the registration was a success.

                if(read == active){
                    //we are lucky, the registration was done successfully and we managed to cas the listener
                    //before the update (since the update hasn't happened yet). This means that the updating thread
                    //is responsible for notifying the listeners.
                    return true;
                }

                //we are even more lucky, because the interesting write already has happened.
                //todo: we can try to remove the created Listeners as object usage optimization
                latch.open(listenerEra);
                return false;
            }else if(listeners == null){
                //an update must have taken place, so no registration needed
                latch.open(listenerEra);
                //
                return false;
            }else if(read != active){
                latch.open(listenerEra);
                return false;
            }

            //so we are contending with another register thread, so lets try it again.
        }
    }

    public final long atomicGet(){
        LongRefTranlocal read = active;
        if(read.isPermanent){
            return read.value;
        }

        read = load(8);
        if(read == null){
            throw new TodoException();
        }

        if(read.locked){
            throw new ReadConflict();
        }

        long value = read.value;
        if(!read.isPermanent && departAfterReading()){
            //Only a non parmenent tranlocal is allowed to do a depart.
            //The orec indicates that it has become time to transform the tranlocal to mark as permanent.

            read.markAsPermanent();

            unlockAfterBecomingReadBiased();
        }
        return value;
    }

    public final long atomicSet(long newValue){
        throw new TodoException();
    }

    public final long atomicSet(
        final long newValue, final ObjectPool pool, final int spinCount,
        final GlobalConflictCounter globalConflictCounter){

        if (!arriveAndLockForUpdate(spinCount)) {
            throw new WriteConflict();
        }

        final LongRefTranlocal oldActive = active;

        if(oldActive.value== newValue){
            if(oldActive.isPermanent){
                unlockByPermanent();
            } else{
                if(departAfterReadingAndReleaseLock()){
                    oldActive.markAsPermanent();
                    unlockAfterBecomingReadBiased();
                }
            }
        }

        //lets create a tranlocal for the update.
        LongRefTranlocal update = pool.take(this);
        if(update == null){
            update = new LongRefTranlocal(this);
        }

        //lets do the update.
        update.value = newValue;
        update.prepareForCommit();
        active = update;
        long remainingSurplus = departAfterUpdateAndReleaseLock(globalConflictCounter, this);
        if (remainingSurplus == 0) {
            //nobody is using the tranlocal anymore, so pool it.
            pool.put(oldActive);
        }

        return oldActive.value;
    }

    //a controlled jmm problem here since identityHashCode is not synchronized/volatile/final.
    //this is the same as with the hashcode and String.
    @Override
    public final int identityHashCode(){
        int tmp = identityHashCode;
        if(tmp != 0){
            return tmp;
        }

        tmp = System.identityHashCode(this);
        identityHashCode = tmp;
        return tmp;
    }
}
