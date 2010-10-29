package org.multiverse.stms.beta.transactions;

import org.multiverse.api.blocking.DefaultRetryLatch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
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

    private Tranlocal attached;
    private boolean hasReads;
    private boolean hasUntrackedReads;
    private LocalConflictCounter localConflictCounter;
    private boolean evaluatingCommute;

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

    public void ensureWrites(){
        if(status != ACTIVE){
            throw abortEnsureWrites();
        }

        if(config.writeLockMode != LOCKMODE_NONE){
            return;
        }

        if(attached == null||attached.isReadonly()){
            return;
        }

        if(!attached.owner.___tryLockAndCheckConflict(this, config.spinCount, attached, false)){
            throw abortOnReadConflict();
        }
    }

    public final <E> E read(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            RefTranlocal<E> tranlocal = (RefTranlocal<E>)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        throw new TodoException();
    }

    private <E> void flattenCommute(
        final BetaRef<E> ref,
        final RefTranlocal<E> tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final <E> RefTranlocal<E> open(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (RefTranlocal<E>)attached;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final <E> RefTranlocal<E> openForRead(final BetaRef<E> ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            RefTranlocal<E> tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final RefTranlocal<E> tranlocal = (RefTranlocal<E>)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            RefTranlocal<E> tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        RefTranlocal<E> tranlocal = (RefTranlocal<E>)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        RefTranlocal<E> tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (RefTranlocal<E>)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            RefTranlocal<E> tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        RefTranlocal<E> tranlocal = (RefTranlocal<E>)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        tranlocal.value = function.call(tranlocal.value);

     }

    public final  int read(BetaIntRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            IntRefTranlocal tranlocal = (IntRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        throw new TodoException();
    }

    private  void flattenCommute(
        final BetaIntRef ref,
        final IntRefTranlocal tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final  IntRefTranlocal open(BetaIntRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (IntRefTranlocal)attached;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final  IntRefTranlocal openForRead(final BetaIntRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            IntRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final IntRefTranlocal tranlocal = (IntRefTranlocal)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            IntRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        IntRefTranlocal tranlocal = (IntRefTranlocal)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        IntRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (IntRefTranlocal)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            IntRefTranlocal tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        IntRefTranlocal tranlocal = (IntRefTranlocal)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        tranlocal.value = function.call(tranlocal.value);

     }

    public final  boolean read(BetaBooleanRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        throw new TodoException();
    }

    private  void flattenCommute(
        final BetaBooleanRef ref,
        final BooleanRefTranlocal tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final  BooleanRefTranlocal open(BetaBooleanRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (BooleanRefTranlocal)attached;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final  BooleanRefTranlocal openForRead(final BetaBooleanRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            BooleanRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            BooleanRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        BooleanRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BooleanRefTranlocal)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            BooleanRefTranlocal tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        tranlocal.value = function.call(tranlocal.value);

     }

    public final  double read(BetaDoubleRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        throw new TodoException();
    }

    private  void flattenCommute(
        final BetaDoubleRef ref,
        final DoubleRefTranlocal tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final  DoubleRefTranlocal open(BetaDoubleRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (DoubleRefTranlocal)attached;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final  DoubleRefTranlocal openForRead(final BetaDoubleRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            DoubleRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            DoubleRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        DoubleRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (DoubleRefTranlocal)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            DoubleRefTranlocal tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        tranlocal.value = function.call(tranlocal.value);

     }

    public final  long read(BetaLongRef ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.getStm()!=config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            LongRefTranlocal tranlocal = (LongRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        throw new TodoException();
    }

    private  void flattenCommute(
        final BetaLongRef ref,
        final LongRefTranlocal tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final  LongRefTranlocal open(BetaLongRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (LongRefTranlocal)attached;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final  LongRefTranlocal openForRead(final BetaLongRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            LongRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final LongRefTranlocal tranlocal = (LongRefTranlocal)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            LongRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        LongRefTranlocal tranlocal = (LongRefTranlocal)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        LongRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (LongRefTranlocal)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            LongRefTranlocal tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        LongRefTranlocal tranlocal = (LongRefTranlocal)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        tranlocal.value = function.call(tranlocal.value);

     }

    private  void flattenCommute(
        final BetaTransactionalObject ref,
        final Tranlocal tranlocal,
        final int lockMode){

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            throw abortOnReadConflict();
        }

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

    public final  Tranlocal open(BetaTransactionalObject ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
           throw abortOpenOnNull();
        }

        if(ref.getStm() != config.stm){
            throw abortOnStmMismatch(ref);
        }

        if(attached!=null && attached.owner == ref){
            return (Tranlocal)attached;
        }

        Tranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        attached = tranlocal;
        return tranlocal;
    }

    @Override
    public final  Tranlocal openForRead(final BetaTransactionalObject ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForReadWhileEvaluatingCommute(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }
            Tranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(hasReadConflict()){
                ref.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                hasUntrackedReads = true;
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final Tranlocal tranlocal = (Tranlocal)attached;

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
                return tranlocal;
            }
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        if(tranlocal.hasDepartObligation()){
            pool.put(tranlocal);
            throw abortOnTooSmallSize(2);
        }

        if(hasReadConflict()){
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUntrackedReads = true;
        return tranlocal;
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

        lockMode = lockMode>=config.writeLockMode? lockMode: config.writeLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            if(!hasReads){
                localConflictCounter.reset();
                hasReads = true;
            }

            Tranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            if(hasReadConflict()){
                tranlocal.owner.___abort(this, tranlocal, pool);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            attached = tranlocal;
            return tranlocal;
        }

        //the transaction has a previous attached reference

        if(attached.owner != ref){
            throw abortOnTooSmallSize(2);
        }

        //the reference is the one we are looking for.
        Tranlocal tranlocal = (Tranlocal)attached;

        if(tranlocal.isCommuting()){
            flattenCommute(ref, tranlocal, lockMode);
            return tranlocal;
        }

        if(tranlocal.getLockMode() < lockMode
            && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
            throw abortOnReadConflict();
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        return tranlocal;
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

        Tranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (Tranlocal)attached;

        if(tranlocal != null){
            if(!tranlocal.isConstructing()){
               throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(2);
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setDirty(true);
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        attached = tranlocal;
        return tranlocal;
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
            Tranlocal tranlocal = pool.take(ref);
            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attached = tranlocal;
            hasUpdates = true;
            return;
        }
        Tranlocal tranlocal = (Tranlocal)attached;
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
        }

        throw new TodoException();

     }

 
    @Override
    public Tranlocal get(BetaTransactionalObject object){
        return attached == null || attached.owner!= object? null: attached;
    }

    @Override
    public Tranlocal locate(BetaTransactionalObject owner){
        if (status != ACTIVE) {
            throw abortLocate(owner);
        }

        if(owner == null){
            throw abortLocateWhenNullReference();
        }

        return attached == null || attached.owner!= owner? null: attached;
    }
     
    // ======================= read conflict =======================================

    private boolean hasReadConflict() {
        if(config.readLockMode != LOCKMODE_NONE || config.inconsistentReadAllowed){
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

        return attached.owner.___hasReadConflict(attached);
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
                        format("[%s] Failed to execute BetaTransaction.abort, reason: the transaction already is committed",
                            config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        final Tranlocal tranlocal = attached;
        if (tranlocal != null) {
            tranlocal.owner.___abort(this, tranlocal, pool);
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
        final Tranlocal tranlocal = attached;
        if (tranlocal != null) {
            if(config.dirtyCheck){
                if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                    tranlocal.calculateIsDirty();
                }
                listeners = attached.owner.___commitDirty(tranlocal, this, pool);
            }else{
                listeners = attached.owner.___commitAll(tranlocal, this, pool);
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
                        format("[%s] Failed to execute BetaTransaction.prepare, reason: the transaction already is aborted",
                            config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.prepare, reason: the transaction already is committed",
                            config.familyName));
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

            if(hasUpdates && config.readLockMode != LOCKMODE_COMMIT){
                final boolean success = config.dirtyCheck
                    ? attached.prepareDirtyUpdates(pool, this, config.spinCount)
                    : attached.prepareAllUpdates(pool, this, config.spinCount);

                if(!success){
                    throw abortOnWriteConflict();
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

    // ============================ retry ===================

    @Override
    public final void retry() {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRetry();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( attached == null){
            throw abortOnNoRetryPossible();
        }

        DefaultRetryLatch listener = pool.takeDefaultRetryLatch();

        try{
            final long listenerEra = listener.getEra();
            final BetaTransactionalObject owner = attached.owner;

            final boolean noRegistration =
                owner.___registerChangeListener(listener, attached, pool, listenerEra) == REGISTRATION_NONE;
            owner.___abort(this, attached, pool);
            attached = null;
            status = ABORTED;

            if(config.permanentListeners != null){
                notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
            }

            if(normalListeners != null){
                notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
            }

            if(noRegistration){
                throw abortOnNoRetryPossible();
            }

            awaitUpdate(listener);
            throw Retry.INSTANCE;
        }finally{
            pool.putDefaultRetryLatch(listener);
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
        hasUpdates = false;
        attempt++;
        abortOnly = false;
        evaluatingCommute = false;
        hasReads = false;
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

        hasUpdates = false;
        status = ACTIVE;
        abortOnly = false;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 1;
        evaluatingCommute = false;
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

