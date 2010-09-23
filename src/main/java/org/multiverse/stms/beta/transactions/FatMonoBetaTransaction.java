package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.*;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import static java.lang.String.format;

/**
 * A BetaTransaction tailored for dealing with 1 transactional object.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class FatMonoBetaTransaction extends AbstractFatBetaTransaction {

    public final static int BITMASK_HAS_UPDATES = 0x1;
    public final static int BITMASK_HAS_READS = 0x2;
    public final static int BITMASK_HAS_UNTRACKED_READS = 0x4;
    public final static int BITMASK_EVALUATING_COMMUTE = 0x8;

    private int state = 0;
    private Tranlocal attached;
//    private boolean hasUpdates;
//    private boolean hasReads;
    private boolean hasUntrackedReads;
    private boolean evaluatingCommute;
    private LocalConflictCounter localConflictCounter;

    public FatMonoBetaTransaction(final BetaStm stm){
        this(new BetaTransactionConfiguration(stm).init());
    }

    public FatMonoBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_FAT_MONO, config);
        this.remainingTimeoutNs = config.timeoutNs;
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
    }

    public final LocalConflictCounter getLocalConflictCounter(){
        return localConflictCounter;
    }

    public final boolean tryLock(BetaTransactionalObject ref, int lockMode){
        throw new TodoException();
    }
    

    private <E> void flattenCommute(
        final BetaRef<E> ref,
        final RefTranlocal<E> tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final RefTranlocal<E> read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final <E> RefTranlocal<E> openForRead(
        final BetaRef<E> ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            RefTranlocal<E> read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
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
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        RefTranlocal<E> read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final <E> RefTranlocal<E> openForWrite(
        final BetaRef<E> ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            RefTranlocal<E> read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            RefTranlocal<E> result = pool.take(ref);
            if (result == null) {
                result = new RefTranlocal<E>(ref);
            }
            result.value = read.value;
            result.read = read;

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        RefTranlocal<E> result = (RefTranlocal<E>)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
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
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        RefTranlocal<E> result = (attached == null || attached.owner != ref) ? null : (RefTranlocal<E>)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
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
        BetaRef<E> ref, Function<E> function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            RefTranlocal<E> result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
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
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaIntRef ref,
        final IntRefTranlocal tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final IntRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final  IntRefTranlocal openForRead(
        final BetaIntRef ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            IntRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
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
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        IntRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  IntRefTranlocal openForWrite(
        final BetaIntRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            IntRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            IntRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new IntRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        IntRefTranlocal result = (IntRefTranlocal)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
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
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        IntRefTranlocal result = (attached == null || attached.owner != ref) ? null : (IntRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
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
        BetaIntRef ref, IntFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            IntRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
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
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaBooleanRef ref,
        final BooleanRefTranlocal tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final BooleanRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final  BooleanRefTranlocal openForRead(
        final BetaBooleanRef ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            BooleanRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            BooleanRefTranlocal result = (BooleanRefTranlocal)attached;

            if(result.isCommuting){
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        BooleanRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  BooleanRefTranlocal openForWrite(
        final BetaBooleanRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            BooleanRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            BooleanRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new BooleanRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        BooleanRefTranlocal result = (BooleanRefTranlocal)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(!result.isCommitted){
            return result;
        }

        final BooleanRefTranlocal read = result;
        result = pool.take(ref);
        if (result == null) {
            result = new BooleanRefTranlocal(ref);
        }
        result.value = read.value;
        result.read = read;
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final  BooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BooleanRefTranlocal result = (attached == null || attached.owner != ref) ? null : (BooleanRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        result =  pool.take(ref);
        if(result == null){
            result = new BooleanRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        BetaBooleanRef ref, BooleanFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            BooleanRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        BooleanRefTranlocal result = (BooleanRefTranlocal)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final BooleanRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new BooleanRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaDoubleRef ref,
        final DoubleRefTranlocal tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final DoubleRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final  DoubleRefTranlocal openForRead(
        final BetaDoubleRef ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            DoubleRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
                attached = read;
            }else{
                hasUntrackedReads = true;
            }

            return read;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.
            DoubleRefTranlocal result = (DoubleRefTranlocal)attached;

            if(result.isCommuting){
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        DoubleRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  DoubleRefTranlocal openForWrite(
        final BetaDoubleRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            DoubleRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            DoubleRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new DoubleRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        DoubleRefTranlocal result = (DoubleRefTranlocal)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(!result.isCommitted){
            return result;
        }

        final DoubleRefTranlocal read = result;
        result = pool.take(ref);
        if (result == null) {
            result = new DoubleRefTranlocal(ref);
        }
        result.value = read.value;
        result.read = read;
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        DoubleRefTranlocal result = (attached == null || attached.owner != ref) ? null : (DoubleRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        BetaDoubleRef ref, DoubleFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            DoubleRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            result.addCommutingFunction(function, pool);
            return;
        }

        DoubleRefTranlocal result = (DoubleRefTranlocal)attached;
        if(result.isCommuting){
            result.addCommutingFunction(function, pool);
            return;
        }

        if(result.isCommitted){
            final DoubleRefTranlocal read = result;
            result =  pool.take(ref);
            if(result == null){
                result = new DoubleRefTranlocal(ref);
            }
            result.read = read;
            result.value = read.value;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaLongRef ref,
        final LongRefTranlocal tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final LongRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final  LongRefTranlocal openForRead(
        final BetaLongRef ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            LongRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
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
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        LongRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  LongRefTranlocal openForWrite(
        final BetaLongRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            LongRefTranlocal read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            LongRefTranlocal result = pool.take(ref);
            if (result == null) {
                result = new LongRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        LongRefTranlocal result = (LongRefTranlocal)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
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
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        LongRefTranlocal result = (attached == null || attached.owner != ref) ? null : (LongRefTranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
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
        BetaLongRef ref, LongFunction function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            LongRefTranlocal result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
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
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
            attached=result;
        }

        result.value = function.call(result.value);
     }


    private  void flattenCommute(
        final BetaTransactionalObject ref,
        final Tranlocal tranlocal,
        final int lockMode){

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        final Tranlocal read = ref.___load(config.spinCount, this, lockMode);

        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            ref.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        tranlocal.read = read;
        boolean abort = true;
        evaluatingCommute = true;
        try{
            tranlocal.evaluateCommutingFunctions(pool);
            abort = false;
        }finally{
            evaluatingCommute = false;
            if(abort){
                abort();
            }
        }
    }

    @Override
    public final  Tranlocal openForRead(
        final BetaTransactionalObject ref,
        int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
//                hasReads = true;
            }
            Tranlocal read = ref.___load(config.spinCount, this, lockMode);

            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }

            if(lockMode!=LOCKMODE_NONE || !read.isPermanent || config.trackReads){
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
                flattenCommute(ref, result, lockMode);
                return result;
            }else
            if(lockMode!=LOCKMODE_NONE &&
                !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return result;
        }

        if(lockMode!=LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if((state & BITMASK_HAS_READS) == 0){
            localConflictCounter.reset();
            state = state | BITMASK_HAS_READS;
        }

        Tranlocal read = ref.___load(config.spinCount, this, lockMode);

        //if it was locked, lets abort.
        if (read.isLocked) {
            throw abortOnReadConflict();
        }

        if(read.isPermanent){
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            read.owner.___abort(this, read, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return read;
    }

    @Override
    public final  Tranlocal openForWrite(
        final BetaTransactionalObject ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if((state & BITMASK_HAS_READS) == 0){
                localConflictCounter.reset();
                state = state | BITMASK_HAS_READS;
            }
            Tranlocal read = ref.___load(config.spinCount, this, lockMode);

            //if it was locked, lets abort.
            if (read.isLocked) {
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                read.owner.___abort(this, read, pool);
                throw abortOnReadConflict();
            }
    
            Tranlocal result = read.openForWrite(pool);

            state = BITMASK_HAS_UPDATES | state;
//            hasUpdates = true;
            attached = result;
            return result;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        Tranlocal result = (Tranlocal)attached;

        if(result.isCommuting){
            flattenCommute(ref, result, lockMode);
            return result;
        }else
        if(lockMode!=LOCKMODE_NONE
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, result, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(!result.isCommitted){
            return result;
        }

        final Tranlocal read = result;
        result = read.openForWrite(pool);
        state = state | BITMASK_HAS_UPDATES;
//        hasUpdates = true;
        attached = result;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        Tranlocal result = (attached == null || attached.owner != ref) ? null : (Tranlocal)attached;

        if(result != null){
            if(result.isCommitted || result.read != null){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___unsafeLoad()!=null){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        result = ref.___openForConstruction(pool);
        result.isDirty = DIRTY_TRUE;
        attached = result;
        return result;
    }

    public  void commute(
        BetaTransactionalObject ref, Function function){

        if (status != ACTIVE) {
            throw abortCommute(ref, function);
        }

        if(function == null){
            throw abortCommuteOnNullFunction(ref);
        }
    
        if(evaluatingCommute){
            throw abortOnCommuteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortCommuteWhenReadonly(ref, function);
        }

        if (ref == null) {
            throw abortCommuteWhenNullReference(function);
        }

        final boolean contains = (attached != null && attached.owner == ref);
        if(!contains){
            if(attached != null) {
                throw abortOnTooSmallSize(2);
            }

            //todo: call to 'openForCommute' can be inlined.
            Tranlocal result = ref.___openForCommute(pool);
            attached=result;
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
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
            state = state | BITMASK_HAS_UPDATES;
//            hasUpdates = true;
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
        if(config.readLockMode!=LOCKMODE_NONE){
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
    public final void abort() {
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
            attached = null;
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
    public final void commit() {
        if(status == COMMITTED){
            return;
        }

        prepare();
        Listeners listeners = null;
        if (attached != null) {
            if(config.dirtyCheck){
                if(attached.isDirty == DIRTY_UNKNOWN){
                    attached.calculateIsDirty();
                }
                listeners = attached.owner.___commitDirty(attached, this, pool);
            }else{
                listeners = attached.owner.___commitAll(attached, this, pool);
            }
            attached = null;
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
    public final void prepare() {
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
                throw abortOnWriteConflict();
            }
          
            if((state & BITMASK_HAS_UPDATES) > 0){
                if(config.dirtyCheck){
                    if(!doPrepareDirty()){
                        throw abortOnWriteConflict();
                    }
                }else{
                    if(!doPrepareAll()){
                        throw abortOnWriteConflict();
                    }
                }
            }

            status = PREPARED;
            abort = false;
        }finally{
            if(abort){
                abort();
            }
        }
    }

    private boolean doPrepareDirty(){
        if(config.writeLockMode==LOCKMODE_COMMIT){
            return true;
        }

        if(attached.isCommitted){
            return true;
        }

        if(attached.isCommuting){
            Tranlocal read = attached.owner.___load(config.spinCount, this, LOCKMODE_COMMIT);

            if(read.isLocked){
                return false;
            }

            attached.read = read;
            attached.evaluateCommutingFunctions(pool);            
        }else
        if (attached.calculateIsDirty()
                    && !attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached, true)){
            return false;
        }

        return true;
    }

    private boolean doPrepareAll(){
        if(config.writeLockMode==LOCKMODE_COMMIT){            
            return true;
        }
        
        if(attached.isCommitted){
            return true;
        }

        if(attached.isCommuting){
            Tranlocal read = attached.owner.___load(config.spinCount, this, LOCKMODE_COMMIT);

            if(read.isLocked){
                return false;
            }

            attached.read = read;
            attached.evaluateCommutingFunctions(pool);
        }else
        if(!attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached, true)){
            return false;
        }

        return true;
    }

    // ============================ registerChangeListenerAndAbort ===================

    @Override
    public final void registerChangeListenerAndAbort(final Latch listener) {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRegisterChangeListenerAndAbort();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( attached == null){
            throw abortOnNoRetryPossible();
        }

        final long listenerEra = listener.getEra();
        final BetaTransactionalObject owner = attached.owner;

        final boolean failure = owner.___registerChangeListener(listener, attached, pool, listenerEra)
                    == REGISTRATION_NONE;
        owner.___abort(this, attached, pool);
        attached = null;
        status = ABORTED;

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
        }

        if(failure){
            throw abortOnNoRetryPossible();
        }
    }

    // =========================== init ================================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort();
        }

        this.config = transactionConfig;
        hardReset();
    }

    // ========================= reset ===============================

    @Override
    public boolean softReset() {
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        if(attempt >= config.getMaxRetries()){
            return false;
        }

        status = ACTIVE;
        status = 0;
//        hasUpdates = false;
        attempt++;
        abortOnly = false;
        evaluatingCommute = false;
//        hasReads = false;
        hasUntrackedReads = false;
        if(normalListeners!=null){
            normalListeners.clear();
        }     
        return true;
    }

    @Override
    public void hardReset(){
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        state = 0;
//        hasUpdates = false;
        status = ACTIVE;
        abortOnly = false;        
        remainingTimeoutNs = config.timeoutNs;
        attempt = 1;
        evaluatingCommute = false;
//        hasReads = false;
        hasUntrackedReads = false;
        if(normalListeners !=null){
            pool.putArrayList(normalListeners);
            normalListeners = null;
        }
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch(){
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch(){
        throw new TodoException();
    }
}

