package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.Function;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import static java.lang.String.format;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

/**
 * A BetaTransaction tailored for dealing with 1 transactional object.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class FatMonoBetaTransaction extends AbstractFatBetaTransaction {

    private Tranlocal attached;
    private boolean hasUpdates;
    private boolean hasReads;
    private boolean hasUntrackedReads;
    private LocalConflictCounter localConflictCounter;

    public FatMonoBetaTransaction(final BetaStm stm){
        this(new BetaTransactionConfiguration(stm));
    }

    public FatMonoBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_FAT_MONO, config);
        this.remainingTimeoutNs = config.timeoutNs;
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
    }

    public final LocalConflictCounter getLocalConflictCounter(){
        return localConflictCounter;
    }


    private <E> void flattenCommute(
        final BetaObjectPool pool,
        final BetaRef<E> ref,
        final RefTranlocal<E> tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final RefTranlocal<E> read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        tranlocal.read = read;
        boolean abort = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
    }

    @Override
    public final <E> RefTranlocal<E> openForRead(final BetaRef<E> ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            if(lock){
                RefTranlocal<E> read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if(hasReadConflict()){
                    read.owner.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            RefTranlocal<E> read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            RefTranlocal<E> result = (RefTranlocal<E>)attached;

            if(result.isCommuting){
                flattenCommute(pool, ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final <E> RefTranlocal<E> openForWrite(
        final BetaRef<E> ref, boolean lock, final BetaObjectPool pool) {

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

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            RefTranlocal<E> read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
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

        if(result.isCommuting){
            flattenCommute(pool, ref, result, lock);
            return result;
        }else
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
        final BetaRef<E> ref, final BetaObjectPool pool) {

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
        BetaRef<E> ref, BetaObjectPool pool, Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(pool, ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(pool, function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(pool, 2);
            }

            //todo: call to 'openForCommute' can be inlined.
            RefTranlocal<E> result = ref.___openForCommute(pool);
            attached=result;
            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        RefTranlocal<E> result = (RefTranlocal<E>)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final RefTranlocal<E> read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new RefTranlocal<E>(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaObjectPool pool,
        final BetaIntRef ref,
        final IntRefTranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final IntRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        tranlocal.read = read;
        boolean abort = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
    }

    @Override
    public final  IntRefTranlocal openForRead(final BetaIntRef ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            if(lock){
                IntRefTranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if(hasReadConflict()){
                    read.owner.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            IntRefTranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            IntRefTranlocal result = (IntRefTranlocal)attached;

            if(result.isCommuting){
                flattenCommute(pool, ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  IntRefTranlocal openForWrite(
        final BetaIntRef ref, boolean lock, final BetaObjectPool pool) {

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

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            IntRefTranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
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

        if(result.isCommuting){
            flattenCommute(pool, ref, result, lock);
            return result;
        }else
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
        final BetaIntRef ref, final BetaObjectPool pool) {

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
        BetaIntRef ref, BetaObjectPool pool, IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(pool, ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(pool, function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(pool, 2);
            }

            //todo: call to 'openForCommute' can be inlined.
            IntRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        IntRefTranlocal result = (IntRefTranlocal)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final IntRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new IntRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaObjectPool pool,
        final BetaLongRef ref,
        final LongRefTranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final LongRefTranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        tranlocal.read = read;
        boolean abort = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
    }

    @Override
    public final  LongRefTranlocal openForRead(final BetaLongRef ref,  boolean lock, final BetaObjectPool pool) {

        if (status != ACTIVE) {
            throw abortOpenForRead(pool, ref);
        }

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            if(lock){
                LongRefTranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if(hasReadConflict()){
                    read.owner.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            LongRefTranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            LongRefTranlocal result = (LongRefTranlocal)attached;

            if(result.isCommuting){
                flattenCommute(pool, ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  LongRefTranlocal openForWrite(
        final BetaLongRef ref, boolean lock, final BetaObjectPool pool) {

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

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            LongRefTranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
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

        if(result.isCommuting){
            flattenCommute(pool, ref, result, lock);
            return result;
        }else
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
        final BetaLongRef ref, final BetaObjectPool pool) {

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
        BetaLongRef ref, BetaObjectPool pool, LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(pool, ref, function);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(pool, ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(pool, function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(pool, 2);
            }

            //todo: call to 'openForCommute' can be inlined.
            LongRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        LongRefTranlocal result = (LongRefTranlocal)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final LongRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new LongRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaObjectPool pool,
        final BetaTransactionalObject ref,
        final Tranlocal tranlocal,
        final boolean lock){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        final Tranlocal read = lock
            ? ref.___lockAndLoad(config.spinCount, this)
            : ref.___load(config.spinCount);

        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        tranlocal.read = read;
        boolean abort = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
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

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            if(lock){
                Tranlocal read = ref.___lockAndLoad(config.spinCount, this);

                //if it was locked, lets abort.
                if (read.isLocked) {
                    throw abortOnReadConflict(pool);
                }

                if(hasReadConflict()){
                    read.owner.___abort(this, read, pool);
                    throw abortOnReadConflict(pool);
                }

                attached = read;
                return read;
            }

            Tranlocal read = ref.___load(config.spinCount);

            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict(pool);
            }

            if(!read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            Tranlocal result = (Tranlocal)attached;

            if(result.isCommuting){
                flattenCommute(pool, ref, result, lock);
                return result;
            }else
            if(lock && !ref.___tryLockAndCheckConflict(this, config.spinCount, result)){
                throw abortOnReadConflict(pool);
            }

            return result;
        }

        if(lock || config.trackReads){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal read = ref.___load(config.spinCount);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict(pool);
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(pool, 2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        hasUntrackedReads = true;
        return read;
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

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            Tranlocal read = lock
                ? ref.___lockAndLoad(config.spinCount, this)
                : ref.___load(config.spinCount);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict(pool);
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
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

        if(result.isCommuting){
            flattenCommute(pool, ref, result, lock);
            return result;
        }else
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

        if (config.readonly) {
            throw abortCommuteWhenReadonly(pool, ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(pool, function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(pool, 2);
            }

            //todo: call to 'openForCommute' can be inlined.
            Tranlocal result = ref.___openForCommute(pool);
            attached=result;
            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        Tranlocal result = (Tranlocal)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final Tranlocal read = result;
            result = read.openForWrite(pool);
            hasUpdates = true;
            attached=result;
        }

        throw new TodoException();
     }

 
    @Override
    public Tranlocal get(BetaTransactionalObject object){
        return attached == null || attached.owner!= object? null: attached;
    }

    // ======================= read conflict =======================================

    private boolean hasReadConflict() {
        if(config.lockReads){
            return false;
        }

        if(hasUntrackedReads){
            return localConflictCounter.syncAndCheckConflict();
        }

        if(attached == null){
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        return attached.owner.___hasReadConflict(attached, this);
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

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
        }
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

        prepare(pool);
        Listeners listeners = null;
        if (attached!=null) {
            if(config.dirtyCheck){
                if(attached.isDirty == DIRTY_UNKNOWN){
                    attached.calculateIsDirty();
                }
                listeners = attached.owner.___commitDirty(attached, this, pool, config.globalConflictCounter);
            }else{
                listeners = attached.owner.___commitAll(attached, this, pool, config.globalConflictCounter);
            }
        }
        status = COMMITTED;

        if(listeners != null){
            listeners.openAll(pool);
        }

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    // ======================= prepare ============================

    @Override
    public void prepare() {
        prepare(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void prepare(final BetaObjectPool pool) {
        if(status != ACTIVE){
            switch (status) {
                case PREPARED:
                    return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare an already aborted transaction", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't prepare an already committed transaction", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        boolean abort = true;
        try{
            if(config.permanentListeners != null){
                notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PrePrepare);
            }

            if(normalListeners != null){
                notifyListeners(normalListeners, TransactionLifecycleEvent.PrePrepare);
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
            abort = false;
        }finally{
            if(abort){
                abort(pool);
            }
        }
    }

    private boolean doPrepareDirty(final BetaObjectPool pool){
        if(config.lockWrites){
            return true;
        }

        if(attached.isCommitted){
            return true;
        }

        if(attached.isCommuting){
            Tranlocal read = attached.owner.___lockAndLoad(config.spinCount, this);

            if(read.isLocked){
                return false;
            }

            attached.read = read;
            attached.evaluateCommutingFunctions(pool);            
        }else
        if (attached.calculateIsDirty()
                    && !attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached)){
            return false;
        }

        return true;
    }

    private boolean doPrepareAll(final BetaObjectPool pool){
        if(config.lockWrites){
            return true;
        }
        
        if(attached.isCommitted){
            return true;
        }

        if(attached.isCommuting){
            Tranlocal read = attached.owner.___lockAndLoad(config.spinCount, this);

            if(read.isLocked){
                return false;
            }

            attached.read = read;
            attached.evaluateCommutingFunctions(pool);
        }else
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

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
        }

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
            abort(pool);
        }

        if(attempt >= config.getMaxRetries()){
            return false;
        }

        status = ACTIVE;
        hasUpdates = false;
        attempt++;
        abortOnly = false;
        attached = null;
        hasReads = false;
        hasUntrackedReads = false;
        if(normalListeners!=null){
            normalListeners.clear();
        }     
        return true;
    }

    @Override
    public void hardReset(){
        hardReset(getThreadLocalBetaObjectPool());
    }

    @Override
    public void hardReset(final BetaObjectPool pool){
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }

        hasUpdates = false;
        status = ACTIVE;
        abortOnly = false;        
        remainingTimeoutNs = config.timeoutNs;
        attached = null;
        attempt = 1;
        hasReads = false;
        hasUntrackedReads = false;
        if(normalListeners !=null){
            pool.putArrayList(normalListeners);
            normalListeners = null;
        }
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        startEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startEitherBranch(BetaObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch(){
        endEitherBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void endEitherBranch(BetaObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch(){
        startOrElseBranch(getThreadLocalBetaObjectPool());
    }

    @Override
    public final void startOrElseBranch(BetaObjectPool pool){
        throw new TodoException();
    }
}

