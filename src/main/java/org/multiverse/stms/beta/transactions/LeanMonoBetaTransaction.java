package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.conflictcounters.*;

import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.*;

import static java.lang.String.format;

/**
 * A BetaTransaction tailored for dealing with 1 transactional object.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class LeanMonoBetaTransaction extends AbstractLeanBetaTransaction {

    private Tranlocal attached;
    private boolean hasUpdates;

    public LeanMonoBetaTransaction(final BetaStm stm){
        this(new BetaTransactionConfiguration(stm));
    }

    public LeanMonoBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_MONO, config);
        this.remainingTimeoutNs = config.timeoutNs;
    }


    @Override
    public final <E> RefTranlocal<E> openForRead(final Ref<E> ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(lock){
                RefTranlocal<E> read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            RefTranlocal<E> read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                throw abortOnTooSmallSize(pool, 2);
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            RefTranlocal<E> result = (RefTranlocal<E>)attached;

            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        RefTranlocal<E> read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        throw abortOnTooSmallSize(pool, 2);
    }

    @Override
    public final <E> RefTranlocal<E> openForWrite(
        final Ref<E> ref, boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(pool, ref);
        }

        lock = lock || config.lockWrites;

        if(attached == null){
            //the transaction has no previous attached references.

            RefTranlocal<E> read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            RefTranlocal<E> result = pool.take(ref);
            if (result == null) {
                result = new RefTranlocal<E>(ref);
            }
            result.value = read.value;
            result.read = read;

            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(pool, 2);
        }

        //the reference is the one we are looking for.
        RefTranlocal<E> result = (RefTranlocal<E>)attached;

        if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
            throw abortOnReadConflict(pool);
        }

        if(!result.isCommitted){
            return result;
        }

        final RefTranlocal<E> read = result;
        result = pool.take(ref);
        if (result == null) {
            result = new RefTranlocal<E>(ref);
        }
        result.value = read.value;
        result.read = read;
        hasUpdates = true;    
        attached = result;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final Ref<E> ref, final BetaObjectPool pool) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(pool, ref);                        
        }

        RefTranlocal<E> result = (attached == null || attached.owner != ref) ? null : (RefTranlocal<E>)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(pool, ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool, 2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(pool, ref);
        }

        result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public <E> void commute(
        Ref<E> ref, BetaObjectPool pool, Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        config.needsCommute();
        abort(pool);
        throw SpeculativeConfigurationError.INSTANCE;
     }

    @Override
    public final  IntRefTranlocal openForRead(final IntRef ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(lock){
                IntRefTranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            IntRefTranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                throw abortOnTooSmallSize(pool, 2);
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            IntRefTranlocal result = (IntRefTranlocal)attached;

            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        IntRefTranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        throw abortOnTooSmallSize(pool, 2);
    }

    @Override
    public final  IntRefTranlocal openForWrite(
        final IntRef ref, boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(pool, ref);
        }

        lock = lock || config.lockWrites;

        if(attached == null){
            //the transaction has no previous attached references.

            IntRefTranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            IntRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new IntRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(pool, 2);
        }

        //the reference is the one we are looking for.
        IntRefTranlocal result = (IntRefTranlocal)attached;

        if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
            throw abortOnReadConflict(pool);
        }

        if(!result.isCommitted){
            return result;
        }

        final IntRefTranlocal read = result;
        result = pool.take(ref);
        if (result == null) {
            result = new IntRefTranlocal(ref);
        }
        result.value = read.value;
        result.read = read;
        hasUpdates = true;    
        attached = result;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final IntRef ref, final BetaObjectPool pool) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(pool, ref);                        
        }

        IntRefTranlocal result = (attached == null || attached.owner != ref) ? null : (IntRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(pool, ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool, 2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(pool, ref);
        }

        result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        IntRef ref, BetaObjectPool pool, IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        config.needsCommute();
        abort(pool);
        throw SpeculativeConfigurationError.INSTANCE;
     }

    @Override
    public final  LongRefTranlocal openForRead(final LongRef ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(lock){
                LongRefTranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            LongRefTranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                throw abortOnTooSmallSize(pool, 2);
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            LongRefTranlocal result = (LongRefTranlocal)attached;

            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        LongRefTranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        throw abortOnTooSmallSize(pool, 2);
    }

    @Override
    public final  LongRefTranlocal openForWrite(
        final LongRef ref, boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(pool, ref);
        }

        lock = lock || config.lockWrites;

        if(attached == null){
            //the transaction has no previous attached references.

            LongRefTranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            LongRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new LongRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(pool, 2);
        }

        //the reference is the one we are looking for.
        LongRefTranlocal result = (LongRefTranlocal)attached;

        if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
            throw abortOnReadConflict(pool);
        }

        if(!result.isCommitted){
            return result;
        }

        final LongRefTranlocal read = result;
        result = pool.take(ref);
        if (result == null) {
            result = new LongRefTranlocal(ref);
        }
        result.value = read.value;
        result.read = read;
        hasUpdates = true;    
        attached = result;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final LongRef ref, final BetaObjectPool pool) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(pool, ref);                        
        }

        LongRefTranlocal result = (attached == null || attached.owner != ref) ? null : (LongRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(pool, ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool, 2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(pool, ref);
        }

        result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        LongRef ref, BetaObjectPool pool, LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        config.needsCommute();
        abort(pool);
        throw SpeculativeConfigurationError.INSTANCE;
     }

    @Override
    public final  Tranlocal openForRead(final BetaTransactionalObject ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(lock){
                Tranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            Tranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                throw abortOnTooSmallSize(pool, 2);
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            Tranlocal result = (Tranlocal)attached;

            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        Tranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        throw abortOnTooSmallSize(pool, 2);
    }

    @Override
    public final  Tranlocal openForWrite(
        final BetaTransactionalObject ref, boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(pool, ref);
        }

        lock = lock || config.lockWrites;

        if(attached == null){
            //the transaction has no previous attached references.

            Tranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            Tranlocal result = read.openForWrite(pool);

            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(pool, 2);
        }

        //the reference is the one we are looking for.
        Tranlocal result = (Tranlocal)attached;

        if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
            throw abortOnReadConflict(pool);
        }

        if(!result.isCommitted){
            return result;
        }

        final Tranlocal read = result;
        result = read.openForWrite(pool);
        hasUpdates = true;    
        attached = result;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref, final BetaObjectPool pool) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(pool, ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference(pool);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(pool, ref);                        
        }

        Tranlocal result = (attached == null || attached.owner != ref) ? null : (Tranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(pool, ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool, 2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(pool, ref);
        }

        result = ref.___openForConstruction(pool);
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        BetaTransactionalObject ref, BetaObjectPool pool, Function function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        config.needsCommute();
        abort(pool);
        throw SpeculativeConfigurationError.INSTANCE;
     }

 
    @Override
    public Tranlocal get(BetaTransactionalObject object){
        return attached == null || attached.owner!= object? null: attached;
    }

    // ============================= addWatch ===================================

    public void addWatch(BetaTransactionalObject object, Watch watch){
        throw new TodoException();
    }


    // ======================= abort =======================================

    @Override
    public void abort() {
        abort(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void abort(final BetaObjectPool pool) {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    return;
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't abort an already aborted transaction",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (attached != null) {
            attached.owner.___abort(this, attached, pool);
        }

        status = ABORTED;

    }

    // ================== commit ===========================================

    @Override
    public void commit() {
        commit(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void commit(final BetaObjectPool pool) {
        if(status == COMMITTED){
            return;
        }

        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't commit an already aborted transaction", config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict(pool);
        }
            
        Listeners listeners = null;
        if(attached!=null){
            final boolean needsPrepare = status == ACTIVE && hasUpdates;
            if(config.dirtyCheck){
                if(needsPrepare && !doPrepareDirty(pool)){
                    throw abortOnWriteConflict(pool);
                }

                listeners = attached.owner.___commitDirty(attached, this, pool, config.globalConflictCounter);
            }else{
                if(needsPrepare && !doPrepareAll(pool)){
                    throw abortOnWriteConflict(pool);
                }

                listeners = attached.owner.___commitAll(attached, this, pool, config.globalConflictCounter);
            }
        }

        status = COMMITTED;

        if(listeners != null){
            listeners.openAll(pool);
        }
    }

    // ======================= prepare ============================

    @Override
    public void prepare() {
        prepare(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void prepare(final BetaObjectPool pool) {
        if(status == PREPARED){
            return;
        }

        if(status != ACTIVE){
            switch (status) {
                case PREPARED:
                    return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already aborted transaction", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare already committed transaction", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict(pool);
        }

        if(hasUpdates){
            if(config.dirtyCheck){
                if(!doPrepareDirty(pool)){
                    throw abortOnWriteConflict(pool);
                }
            }else{
                if(!doPrepareAll(pool)){
                    throw abortOnWriteConflict(pool);
                }
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareDirty(final BetaObjectPool pool){
        //if(config.lockWrites){
        //    return true;
        //}

        if(attached.isCommitted){
            return true;
        }

        if (attached.calculateIsDirty()
                    && !attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached)){
            return false;
        }

        return true;
    }

    private boolean doPrepareAll(final BetaObjectPool pool){
        //if(config.lockWrites){
        //    return true;
        //}
        
        if(attached.isCommitted){
            return true;
        }

        if(!attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached)){
            return false;
        }

        return true;
    }

    // ============================ registerChangeListenerAndAbort ===================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener){
        registerChangeListenerAndAbort(listener, getThreadLocalBetaObjectPool());
    }

    @Override
    public final void registerChangeListenerAndAbort(final Latch listener, final BetaObjectPool pool) {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRegisterChangeListenerAndAbort(pool);
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed(pool);
        }

        if( attached == null){
            throw abortOnNoRetryPossible(pool);
        }

        final long listenerEra = listener.getEra();
        final BetaTransactionalObject owner = attached.owner;

        final boolean failure = owner.___registerChangeListener(listener, attached, pool, listenerEra)
            == REGISTRATION_NONE;
        owner.___abort(this, attached, pool);
        status = ABORTED;

        if(failure){
            throw abortOnNoRetryPossible(pool);
        }
    }

    // =========================== init ================================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        init(transactionConfig, getThreadLocalBetaObjectPool());
    }

    @Override
    public void init(BetaTransactionConfiguration transactionConfig, BetaObjectPool pool){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort(pool);
        }

        this.config = transactionConfig;
        hardReset(pool);
    }

    // ========================= reset ===============================

    @Override
    public boolean softReset(){
        return softReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public boolean softReset(final BetaObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            if(attached!=null){
                attached.owner.___abort(this, attached, pool);
            }
        }

        if(attempt >= config.getMaxRetries()){
            return false;
        }

        status = ACTIVE;
        hasUpdates = false;
        attempt++;
        abortOnly = false;
        attached = null;
        return true;
    }

    @Override
    public void hardReset(){
        hardReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public void hardReset(final BetaObjectPool pool){
        if (status == ACTIVE || status == PREPARED) {
            if(attached!=null){
                attached.owner.___abort(this, attached, pool);
            }
        }

        hasUpdates = false;
        status = ACTIVE;
        abortOnly = false;        
        remainingTimeoutNs = config.timeoutNs;
        attached = null;
        attempt = 1;
    }

    // ================== orelse ============================

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        startEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startEitherBranch(BetaObjectPool pool){
        config.needsOrelse();
        abort(pool);
        throw SpeculativeConfigurationError.INSTANCE;
    }

    @Override
    public final void endEitherBranch(){
        endEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void endEitherBranch(BetaObjectPool pool){
        abort(pool);
        throw new IllegalStateException();
    }

    @Override
    public final void startOrElseBranch(){
        startOrElseBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startOrElseBranch(BetaObjectPool pool){
        abort(pool);
        throw new IllegalStateException();
    }
}

