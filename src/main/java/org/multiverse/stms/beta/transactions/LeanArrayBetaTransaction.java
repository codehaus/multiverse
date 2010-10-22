package org.multiverse.stms.beta.transactions;

import org.multiverse.api.Watch;
import org.multiverse.api.blocking.CheapLatch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.functions.*;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.*;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public final class LeanArrayBetaTransaction extends AbstractLeanBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final Tranlocal[] array;
    private LocalConflictCounter localConflictCounter;
    private int firstFreeIndex = 0;
    private boolean hasReads;
    private boolean hasUntrackedReads;

    public LeanArrayBetaTransaction(final BetaStm stm) {
        this(new BetaTransactionConfiguration(stm).init());
    }

    public LeanArrayBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_ARRAY, config);
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.maxArrayTransactionSize];
        this.remainingTimeoutNs = config.timeoutNs;
    }

    @Override
    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    public void ensureWrites(){
        if(status != ACTIVE){
            throw abortEnsureWrites();
        }

        if(config.writeLockMode!=LOCKMODE_NONE){
            return;
        }

        if(firstFreeIndex == 0){
            return;
        }

        final int spinCount = config.spinCount;
        for(int k=0;k<firstFreeIndex;k++){
            final Tranlocal tranlocal = array[k];

            if(tranlocal.isReadonly()){
                continue;
            }

            if(!tranlocal.owner.___tryLockAndCheckConflict(this, spinCount, tranlocal, false)){
                throw abortOnReadConflict();
            }
        }
    }

    @Override
    public final boolean tryLock(BetaTransactionalObject ref, int lockMode){
       if (status != ACTIVE) {
           throw abortTryLock(ref);
       }

       if (ref == null) {
           throw abortTryLockWhenNullReference(ref);
       }

       lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;

       throw new TodoException();
    }


    public final <E> E read(BetaRef<E> ref){
        throw new TodoException();
    }

    public final <E> RefTranlocal<E> open(BetaRef<E> ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (RefTranlocal<E>)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new RefTranlocal<E>(ref);
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
        final BetaRef<E> ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            RefTranlocal<E> tranlocal = (RefTranlocal<E>)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new RefTranlocal<E>(ref);
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public <E> RefTranlocal<E> openForWrite(
        final BetaRef<E>  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            RefTranlocal<E> tranlocal = (RefTranlocal<E>)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        RefTranlocal<E> tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new RefTranlocal<E>(ref);
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        RefTranlocal<E> tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = new RefTranlocal<E>(ref);
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }


    public final  int read(BetaIntRef ref){
        throw new TodoException();
    }

    public final  IntRefTranlocal open(BetaIntRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (IntRefTranlocal)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new IntRefTranlocal(ref);
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public  IntRefTranlocal openForRead(
        final BetaIntRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            IntRefTranlocal tranlocal = (IntRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new IntRefTranlocal(ref);
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  IntRefTranlocal openForWrite(
        final BetaIntRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            IntRefTranlocal tranlocal = (IntRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        IntRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new IntRefTranlocal(ref);
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        IntRefTranlocal tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = new IntRefTranlocal(ref);
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }


    public final  boolean read(BetaBooleanRef ref){
        throw new TodoException();
    }

    public final  BooleanRefTranlocal open(BetaBooleanRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (BooleanRefTranlocal)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new BooleanRefTranlocal(ref);
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public  BooleanRefTranlocal openForRead(
        final BetaBooleanRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new BooleanRefTranlocal(ref);
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  BooleanRefTranlocal openForWrite(
        final BetaBooleanRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            BooleanRefTranlocal tranlocal = (BooleanRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        BooleanRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new BooleanRefTranlocal(ref);
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  BooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            BooleanRefTranlocal result = (BooleanRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        BooleanRefTranlocal tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = new BooleanRefTranlocal(ref);
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }


    public final  double read(BetaDoubleRef ref){
        throw new TodoException();
    }

    public final  DoubleRefTranlocal open(BetaDoubleRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (DoubleRefTranlocal)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new DoubleRefTranlocal(ref);
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public  DoubleRefTranlocal openForRead(
        final BetaDoubleRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new DoubleRefTranlocal(ref);
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  DoubleRefTranlocal openForWrite(
        final BetaDoubleRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            DoubleRefTranlocal tranlocal = (DoubleRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        DoubleRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new DoubleRefTranlocal(ref);
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        DoubleRefTranlocal tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = new DoubleRefTranlocal(ref);
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }


    public final  long read(BetaLongRef ref){
        throw new TodoException();
    }

    public final  LongRefTranlocal open(BetaLongRef ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (LongRefTranlocal)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new LongRefTranlocal(ref);
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public  LongRefTranlocal openForRead(
        final BetaLongRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            LongRefTranlocal tranlocal = (LongRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new LongRefTranlocal(ref);
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  LongRefTranlocal openForWrite(
        final BetaLongRef  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            LongRefTranlocal tranlocal = (LongRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        LongRefTranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = new LongRefTranlocal(ref);
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        LongRefTranlocal tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = new LongRefTranlocal(ref);
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }



    public final  Tranlocal open(BetaTransactionalObject ref){
        if (status != ACTIVE) {
            throw abortOpen(ref);
        }

        if(ref == null){
            throw abortOpenOnNull();
        }
            
        if(ref.getStm()!=config.stm){
            throw abortOnStmMismatch(ref);
        }
                        
        final int index = indexOf(ref);
        if(index != -1){
            return (Tranlocal)array[index];
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        Tranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = ref.___newTranlocal();
        }

        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);
        tranlocal.tx = this;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        //todo: needs to go.
        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode?lockMode:config.readLockMode;
        final int index = indexOf(ref);
        if(index > -1){
            //we are lucky, at already is attached to the session
            Tranlocal tranlocal = (Tranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if (tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){

                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = ref.___newTranlocal();
        }

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            pool.put(tranlocal);
            throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_READONLY);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            ref.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        if( lockMode != LOCKMODE_NONE || config.trackReads || tranlocal.hasDepartObligation()){
            array[firstFreeIndex] = tranlocal;
            firstFreeIndex++;
        }else{
            //todo: pooling of tranlocal
            hasUntrackedReads = true;
        }

        return tranlocal;
    }


    @Override
    public  Tranlocal openForWrite(
        final BetaTransactionalObject  ref, int lockMode) {

        if (status != ACTIVE) {
           throw abortOpenForWrite(ref);
        }

        if (config.readonly) {
            throw abortOpenForWriteWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForWriteWhenNullReference();
        }

        lockMode = lockMode>=config.writeLockMode?lockMode:config.writeLockMode;
        final int index = indexOf(ref);
        if(index != -1){
            Tranlocal tranlocal = (Tranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = tranlocal;
            }

            if(tranlocal.getLockMode() < lockMode
                 && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal,lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            if(tranlocal.isReadonly()){
                hasUpdates = true;
                tranlocal.setStatus(STATUS_UPDATE);
            }

            return tranlocal;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        if(!hasReads){
            localConflictCounter.reset();
            hasReads = true;
        }

        Tranlocal tranlocal = pool.take(ref);
        if(tranlocal == null){
            tranlocal = ref.___newTranlocal();
        }

        if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
           pool.put(tranlocal);
           throw abortOnReadConflict();
        }

        tranlocal.setStatus(STATUS_UPDATE);
        tranlocal.setIsConflictCheckNeeded(!config.writeSkewAllowed);

        if (hasReadConflict()) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            throw abortOnReadConflict();
        }

        hasUpdates = true;
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
        return tranlocal;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
            throw abortOpenForConstruction(ref);
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        final int index = indexOf(ref);
        if(index >= 0){
            Tranlocal result = (Tranlocal)array[index];

            if(!result.isConstructing()){
                throw abortOpenForConstructionWithBadReference(ref);
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }

        //it was not previously attached to this transaction

        if(ref.___getLockOwner()!=this && ref.getVersion()!=BetaTransactionalObject.VERSION_UNCOMMITTED){
            throw abortOpenForConstructionWithBadReference(ref);
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(array.length+1);
        }

        //open the tranlocal for writing.
        Tranlocal tranlocal =  pool.take(ref);
        if(tranlocal == null){
                tranlocal = ref.___newTranlocal();
        }
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.setDirty(true);
        array[firstFreeIndex] = tranlocal;
        firstFreeIndex++;
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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
  }

 
    @Override
    public Tranlocal get(BetaTransactionalObject owner){
        int indexOf = indexOf(owner);
        return indexOf == -1 ? null: array[indexOf];
    }

    /**
     * Finds the index of the tranlocal that has the ref as owner. Return -1 if not found.
     *
     * @param owner the owner of the tranlocal to look for.
     * @return the index of the tranlocal, or -1 if not found.
     */
    private int indexOf(BetaTransactionalObject owner){
        assert owner!=null;

        for(int k=0; k < firstFreeIndex; k++){
            final Tranlocal tranlocal = array[k];
            if(tranlocal.owner == owner){
                return k;
            }
        }

        return -1;
    }

    private boolean hasReadConflict() {
        if (config.readLockMode!=LOCKMODE_NONE||config.inconsistentReadAllowed) {
            return false;
        }

        if(hasUntrackedReads){
            return localConflictCounter.syncAndCheckConflict();
        }

        if(firstFreeIndex == 0){
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if (tranlocal.owner.___hasReadConflict(tranlocal)) {
                return true;
            }
        }

        return false;
    }

    // ============================= addWatch ===================================

    public void addWatch(BetaTransactionalObject object, Watch watch){
        throw new TodoException();
    }

    // ============================== abort ==================================

    @Override
    public void abort() {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                status = ABORTED;
                for (int k = 0; k < firstFreeIndex; k++) {
                    final Tranlocal tranlocal = array[k];
                    array[k]=null;
                    tranlocal.owner.___abort(this, tranlocal, pool);
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

    // ================================== commit =================================

    @Override
    public final void commit() {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Can't commit an already aborted transaction",config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict();
        }

        Listeners[] listeners = null;

        if (firstFreeIndex > 0) {
            final boolean needsPrepare = status == ACTIVE
                    && hasUpdates
                    && config.readLockMode != LOCKMODE_COMMIT;

            if(config.dirtyCheck){
                if(needsPrepare && !doPrepareDirty()){
                    throw abortOnWriteConflict();
                }

                listeners = commitDirty();
            }else{
                if(needsPrepare && !doPrepareAll()){
                     throw abortOnWriteConflict();
                }

                listeners = commitAll();
            }
        }

        status = COMMITTED;

        if(listeners != null){
            Listeners.openAll(listeners, pool);
        }
    }

    private Listeners[] commitAll() {
        Listeners[] listenersArray = null;

        int listenersArrayIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];
            array[k] = null;

            final Listeners listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if(listenersArray == null){
                        listenersArray = new Listeners[length];
                    }
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
        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];
            array[k] = null;

            //we need to make sure that the dirty flag is set since it could happen that the
            //prepare completes before setting the dirty flags
            if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                tranlocal.calculateIsDirty();
            }

            final Listeners listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);

            if(listeners != null){
                if(listenersArray == null){
                    final int length = firstFreeIndex - k;
                    listenersArray = pool.takeListenersArray(length);
                    if(listenersArray == null){
                        listenersArray = new Listeners[length];
                    }
                }
                listenersArray[listenersArrayIndex]=listeners;
                listenersArrayIndex++;
            }
        }

        return listenersArray;
    }

    // ========================= prepare ================================


    @Override
    public void prepare() {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     //won't harm to call it more than once.
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
            throw abortOnWriteConflict();
        }

        if(hasUpdates && config.readLockMode != LOCKMODE_COMMIT){
            final boolean success = config.dirtyCheck ? doPrepareDirty() : doPrepareAll();
            if(!success){
                throw abortOnWriteConflict();
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareAll() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if(!tranlocal.prepareAllUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        final int spinCount = config.spinCount;

        for (int k = 0; k < firstFreeIndex; k++) {
            final Tranlocal tranlocal = array[k];

            if(!tranlocal.prepareDirtyUpdates(pool, this, spinCount)) {
                return false;
            }
        }

        return true;
    }

    // ============================== retry ========================

    @Override
    public void retry() {
        if (status != ACTIVE) {
            throw abortOnFaultyStatusOfRetry();
        }

        if(!config.blockingAllowed){
            throw abortOnNoBlockingAllowed();
        }

        if( firstFreeIndex == 0){
            throw abortOnNoRetryPossible();
        }

        CheapLatch listener = pool.takeCheapLatch();
        if(listener == null){
            listener = new CheapLatch();
        }

        try{
            final long listenerEra = listener.getEra();

            boolean furtherRegistrationNeeded = true;
            boolean atLeastOneRegistration = false;

            for(int k=0; k < firstFreeIndex; k++){

                final Tranlocal tranlocal = array[k];
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
                }

                owner.___abort(this, tranlocal, pool);
                array[k]=null;
            }

            status = ABORTED;

            if(!atLeastOneRegistration){
                throw abortOnNoRetryPossible();
            }

            awaitUpdate(listener);
            throw Retry.INSTANCE;
        }finally{
            pool.putCheapLatch(listener);
        }
    }

    // ==================== reset ==============================

    @Override
    public final boolean softReset() {
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }

        if(attempt>=config.getMaxRetries()){
            return false;
        }

        status = ACTIVE;
        abortOnly = false;
        attempt++;
        firstFreeIndex = 0;
        hasReads = false;
        hasUntrackedReads = false;
        hasUpdates = false;
        return true;
    }

    @Override
    public void hardReset(){
        if (status == ACTIVE || status == PREPARED) {
            abort();
        }
        status = ACTIVE;
        abortOnly = false;
        hasReads = false;
        hasUpdates = false;
        hasUntrackedReads = false;
        attempt=1;
        firstFreeIndex = 0;
        remainingTimeoutNs = config.timeoutNs;
    }

    // ==================== init =============================

    @Override
    public void init(BetaTransactionConfiguration transactionConfig){
        if(transactionConfig == null){
            abort();
            throw new NullPointerException();
        }

        if(status == ACTIVE || status == PREPARED){
            abort();
        }

        config = transactionConfig;
        hardReset();
    }

    // ================== orelse ============================

}
