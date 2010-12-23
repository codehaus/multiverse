package org.multiverse.stms.beta.transactions;

import java.util.*;

import org.multiverse.api.*;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.conflictcounters.*;

import java.util.concurrent.atomic.AtomicLong;
import static java.lang.String.format;


/**
 * A {@link BetaTransaction} for arbitrary size transactions.
 *
 * @author Peter Veentjer.
 */
public final class FatArrayTreeBetaTransaction extends AbstractFatBetaTransaction {

    private BetaTranlocal[] array;
    private LocalConflictCounter localConflictCounter;
    private int size;
    private boolean hasReads;
    private boolean hasUntrackedReads;
    private boolean evaluatingCommute;

    public FatArrayTreeBetaTransaction(BetaStm stm) {
        this(new BetaTransactionConfiguration(stm).init());
    }

    public FatArrayTreeBetaTransaction(BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_FAT_ARRAYTREE, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new BetaTranlocal[config.minimalArrayTreeSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    @Override
    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    public int size(){
        return size;
    }

    public float getUsage(){
        return (size * 1.0f)/array.length;
    }

    public final boolean tryLock(BetaTransactionalObject ref, int lockMode){
        throw new TodoException();
    }

    public void ensureWrites(){
        if(status != ACTIVE){
            throw abortEnsureWrites();
        }

        if(config.writeLockMode!=LOCKMODE_NONE){
            return;
        }

        if(size == 0){
            return;
        }

        final int spinCount = config.spinCount;
        for(int k=0;k<array.length;k++){
            final BetaTranlocal tranlocal = array[k];

            if(tranlocal==null || tranlocal.isReadonly()){
                continue;
            }

            if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal, false)){
                throw abortOnReadConflict();
            }
        }
    }

    public final <E> E read(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortRead(ref);
        }

        if(ref == null){
            throw abortReadOnNull();
        }

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index != -1){
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    private <E> void flattenCommute(
        final BetaRef<E> ref,
        final BetaRefTranlocal<E> tranlocal,
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

    @Override
    public <E> BetaRefTranlocal<E> openForRead(
        final BetaRef<E> ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaRefTranlocal<E> tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public <E> BetaRefTranlocal<E> openForWrite(
        final BetaRef<E>  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaRefTranlocal<E> tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final <E> BetaRefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaRefTranlocal<E> tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public <E> void commute(
        final BetaRef<E> ref, final Function<E> function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaRefTranlocal<E> tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
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

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index != -1){
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    private  void flattenCommute(
        final BetaIntRef ref,
        final BetaIntRefTranlocal tranlocal,
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

    @Override
    public  BetaIntRefTranlocal openForRead(
        final BetaIntRef ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaIntRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BetaIntRefTranlocal openForWrite(
        final BetaIntRef  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaIntRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BetaIntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaIntRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public  void commute(
        final BetaIntRef ref, final IntFunction function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaIntRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
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

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index != -1){
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    private  void flattenCommute(
        final BetaBooleanRef ref,
        final BetaBooleanRefTranlocal tranlocal,
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

    @Override
    public  BetaBooleanRefTranlocal openForRead(
        final BetaBooleanRef ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaBooleanRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BetaBooleanRefTranlocal openForWrite(
        final BetaBooleanRef  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaBooleanRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BetaBooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaBooleanRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public  void commute(
        final BetaBooleanRef ref, final BooleanFunction function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaBooleanRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
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

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index != -1){
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    private  void flattenCommute(
        final BetaDoubleRef ref,
        final BetaDoubleRefTranlocal tranlocal,
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

    @Override
    public  BetaDoubleRefTranlocal openForRead(
        final BetaDoubleRef ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaDoubleRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BetaDoubleRefTranlocal openForWrite(
        final BetaDoubleRef  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaDoubleRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BetaDoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaDoubleRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public  void commute(
        final BetaDoubleRef ref, final DoubleFunction function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaDoubleRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
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

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index != -1){
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            hasUntrackedReads = true;
            return ref.atomicWeakGet();
        }
    }

    private  void flattenCommute(
        final BetaLongRef ref,
        final BetaLongRefTranlocal tranlocal,
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

    @Override
    public  BetaLongRefTranlocal openForRead(
        final BetaLongRef ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaLongRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BetaLongRefTranlocal openForWrite(
        final BetaLongRef  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaLongRefTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BetaLongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaLongRefTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public  void commute(
        final BetaLongRef ref, final LongFunction function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaLongRefTranlocal tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
         }

         tranlocal.value = function.call(tranlocal.value);
     }

    private  void flattenCommute(
        final BetaTransactionalObject ref,
        final BetaTranlocal tranlocal,
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

    @Override
    public  BetaTranlocal openForRead(
        final BetaTransactionalObject ref, int lockMode) {

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
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if (index > -1) {
            //we are lucky, at already is attached to the session
            BetaTranlocal tranlocal = (BetaTranlocal)array[index];


            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount,tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode,tranlocal)){
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
            attach(ref, tranlocal, identityHashCode);
            size++;
        }else{
            //todo: pooling
            hasUntrackedReads = true;
        }

        return tranlocal;
    }

    @Override
    public  BetaTranlocal openForWrite(
        final BetaTransactionalObject  ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForWriteWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        lockMode = lockMode>= config.writeLockMode?lockMode:config.writeLockMode;

        if(index >- 1){
            BetaTranlocal tranlocal = (BetaTranlocal)array[index];

            if(tranlocal.isCommuting()){
                flattenCommute(ref, tranlocal, lockMode);
            }else
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

        //it was not previously attached to this transaction

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BetaTranlocal tranlocal = pool.take(ref);
        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        hasUpdates = true;
        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    @Override
    public final  BetaTranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if(evaluatingCommute){
            throw abortOnOpenForConstructionWhileEvaluatingCommute(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);

        if(index >- 1){
            final BetaTranlocal tranlocal = (BetaTranlocal)array[index];
            if(!tranlocal.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            return tranlocal;
        }

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        BetaTranlocal tranlocal =  pool.take(ref);

        tranlocal.tx = this;
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);

        attach(ref, tranlocal, identityHashCode);
        size++;
        return tranlocal;
    }

    public  void commute(
        final BetaTransactionalObject ref, final Function function){

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

        final int identityHashCode = ref.___identityHashCode();
        final int index = indexOf(ref, identityHashCode);
        if(index == -1){
            BetaTranlocal tranlocal = pool.take(ref);
            tranlocal.setStatus(STATUS_COMMUTING);
            tranlocal.addCommutingFunction(function, pool);
            attach(ref, tranlocal, identityHashCode);
            size++;
            hasUpdates = true;
            return;
        }

        BetaTranlocal tranlocal = (BetaTranlocal)array[index];
        if(tranlocal.isCommuting()){
            tranlocal.addCommutingFunction(function, pool);
            return;
        }

        if(tranlocal.isReadonly()){
            tranlocal.setStatus(STATUS_UPDATE);
            hasUpdates = true;
            array[index]=tranlocal;
         }

         throw new TodoException();
     }

 
    @Override
    public BetaTranlocal get(BetaTransactionalObject ref){
        final int indexOf = indexOf(ref, ref.___identityHashCode());
        return indexOf == -1? null: array[indexOf];
    }

    @Override
    public BetaTranlocal locate(BetaTransactionalObject owner){
        if (status != ACTIVE) {
           throw abortLocate(owner);
        }

        if(owner == null){
            throw abortLocateWhenNullReference();
        }

        final int indexOf = indexOf(owner, owner.___identityHashCode());
        return indexOf == -1? null: array[indexOf];
    }

    public int indexOf(final BetaTransactionalObject ref, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            final int offset = goLeft?-jump:jump;
            final int index = (hash + offset) % array.length;

            final BetaTranlocal current = array[index];
            if(current == null){
                return -1;
            }

            if(current.owner == ref){
                return index;
            }

            final int currentHash = current.owner.___identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump*2;
        }while(jump < array.length);

        return -1;
    }

    private void attach(final BetaTransactionalObject ref, final BetaTranlocal tranlocal, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            final int offset = goLeft?-jump:jump;
            final int index = (hash + offset) % array.length;

            BetaTranlocal current = array[index];
            if(current == null){
                array[index] = tranlocal;
                return;
            }

            final int currentHash = current.owner.___identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0?1:jump*2;
        }while(jump < array.length);

        expand();
        attach(ref, tranlocal, hash);
    }

    private void expand(){
        BetaTranlocal[] oldArray = array;
        int newSize = oldArray.length*2;
        array = pool.takeTranlocalArray(newSize);

        for(int k=0; k < oldArray.length; k++){
            final BetaTranlocal tranlocal = oldArray[k];

            if(tranlocal != null){
               attach(tranlocal.owner, tranlocal, tranlocal.owner.___identityHashCode());
            }
        }

        pool.putTranlocalArray(oldArray);
    }

    private boolean hasReadConflict() {
        if(config.readLockMode!=LOCKMODE_NONE || config.inconsistentReadAllowed) {
            return false;
        }

        if(hasUntrackedReads){
            return localConflictCounter.syncAndCheckConflict();
        }

        if(size == 0){
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < array.length; k++) {
            final BetaTranlocal tranlocal = array[k];
            if (tranlocal != null && tranlocal.owner.___hasReadConflict(tranlocal)) {
                return true;
            }
        }

        return false;
    }

     // ============================== abort ===================================================

    @Override
    public void abort() {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;
                if(size > 0){
                    for (int k = 0; k < array.length; k++) {
                        final BetaTranlocal tranlocal = array[k];
                        if(tranlocal != null){
                            array[k] = null;
                            tranlocal.owner.___abort(this, tranlocal, pool);
                        }
                    }
                }

                if(config.permanentListeners != null){
                    notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);
                }

                if(normalListeners != null){
                    notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);
                }
              break;
          case ABORTED:
              break;
            case COMMITTED:
                throw new DeadTransactionException(
                    format("[%s] Can't abort an already committed transaction",config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    // ============================== commit ==================================================

    @Override
    public void commit() {
        if(status == COMMITTED){
            return;
        }

        prepare();

        Listeners[] listenersArray = null;

        if(size > 0){
            listenersArray = config.dirtyCheck ? commitDirty() : commitAll();
        }

        status = COMMITTED;

        if(listenersArray != null){
            Listeners.openAll(listenersArray, pool);
        }

        if(config.permanentListeners != null){
            notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    private Listeners[] commitAll() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            BetaTranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            array[k] = null;
            final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    int length = array.length - k;
                    listenersArray = pool.takeListenersArray(length);
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < array.length; k++) {
            BetaTranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            array[k] = null;

            if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                tranlocal.calculateIsDirty();
            }

            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    int length = array.length - k;
                    listenersArray = pool.takeListenersArray(length);
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
            }
        }

        return listenersArray;
    }

    // ============================== prepare ==================================================

    @Override
    public void prepare() {
        if (status != ACTIVE) {
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
                final boolean success = config.dirtyCheck ? doPrepareDirty():doPrepareAll();
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

    private boolean doPrepareAll() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            final BetaTranlocal tranlocal = array[k];

            if (tranlocal == null){
                continue;
            }

            if(!tranlocal.prepareAllUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < array.length; k++) {
            final BetaTranlocal tranlocal = array[k];

            if(tranlocal == null){
                continue;
            }

            if(!tranlocal.prepareDirtyUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    // ============================ registerChangeListener ===============================

    @Override
    public void retry() {
        if (status != ACTIVE) throw abortOnFaultyStatusOfRetry();

        if(!config.blockingAllowed) throw abortOnNoBlockingAllowed();

        if( size == 0) throw abortOnNoRetryPossible();

        final long listenerEra = listener.getEra();
        boolean furtherRegistrationNeeded = true;
        boolean atLeastOneRegistration = false;
        for(int k=0; k < array.length; k++){
            final BetaTranlocal tranlocal = array[k];

            if(tranlocal == null) continue;

            array[k]=null;
            final BetaTransactionalObject owner = tranlocal.owner;

            if(furtherRegistrationNeeded){
                switch(owner.___registerChangeListener(listener, tranlocal, pool, listenerEra)){
                    case REGISTRATION_DONE:
                        atLeastOneRegistration = true;
                        break;
                    case REGISTRATION_NOT_NEEDED:
                        furtherRegistrationNeeded = false;
                        atLeastOneRegistration = true;
                        break;
                    case REGISTRATION_NONE:
                        break;
                    default:
                        throw new IllegalStateException();
                }

                owner.___abort(this, tranlocal, pool);
            }
        }

        status = ABORTED;
        if(config.permanentListeners != null) notifyListeners(config.permanentListeners, TransactionLifecycleEvent.PostAbort);

        if(normalListeners != null) notifyListeners(normalListeners, TransactionLifecycleEvent.PostAbort);

        if(!atLeastOneRegistration)throw abortOnNoRetryPossible();

        throw Retry.INSTANCE;
    }

    // ============================== reset ========================================

    @Override
    public boolean softReset() {
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        if(attempt>=config.getMaxRetries()){
            return false;
        }

        if(array.length > config.minimalArrayTreeSize){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.minimalArrayTreeSize);
        }

        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUpdates = false;
        hasUntrackedReads = false;
        size = 0;
        attempt++;
        evaluatingCommute = false;
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

        if(array.length>config.minimalArrayTreeSize){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.minimalArrayTreeSize);
        }

        status = ACTIVE;
        abortOnly = false;
        hasUpdates = false;
        hasReads = false;
        hasUntrackedReads = false;
        attempt = 1;
        remainingTimeoutNs = config.timeoutNs;
        size = 0;
        evaluatingCommute = false;
        if(normalListeners !=null){
            pool.putArrayList(normalListeners);
            normalListeners = null;
        }
    }

    // ============================== init =======================================

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

    // ================== orelse ============================

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
