package org.multiverse.stms.beta.transactions;

import org.multiverse.api.*;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.conflictcounters.*;

import static java.lang.String.format;

/**
 * A BetaTransaction tailored for dealing with 1 transactional object.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class LeanMonoBetaTransaction extends AbstractLeanBetaTransaction {

    private BetaTranlocal attached;

    public LeanMonoBetaTransaction(final BetaStm stm){
        this(new BetaTransactionConfiguration(stm).init());
    }

    public LeanMonoBetaTransaction(final BetaTransactionConfiguration config) {
        super(POOL_TRANSACTIONTYPE_LEAN_MONO, config);
        this.remainingTimeoutNs = config.timeoutNs;
    }

    public final LocalConflictCounter getLocalConflictCounter(){
        return null;
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

        if(ref.___stm != config.stm){
            throw abortReadOnStmMismatch(ref);
        }

        if(attached != null && attached.owner == ref){
            BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            throw abortOnTooSmallSize(2);
        }
    }


    @Override
    public final <E> BetaRefTranlocal<E> openForRead(final BetaRef<E> ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaRefTranlocal<E> tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaRefTranlocal<E> tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final <E> BetaRefTranlocal<E> openForWrite(
        final BetaRef<E> ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaRefTranlocal<E> tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaRefTranlocal<E> tranlocal = (BetaRefTranlocal<E>)attached;

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
    public final <E> BetaRefTranlocal<E> openForConstruction(
        final BetaRef<E> ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaRefTranlocal<E> tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaRefTranlocal<E>)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
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

        if(attached != null && attached.owner == ref){
            BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            throw abortOnTooSmallSize(2);
        }
    }


    @Override
    public final  BetaIntRefTranlocal openForRead(final BetaIntRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaIntRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaIntRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final  BetaIntRefTranlocal openForWrite(
        final BetaIntRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaIntRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaIntRefTranlocal tranlocal = (BetaIntRefTranlocal)attached;

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
    public final  BetaIntRefTranlocal openForConstruction(
        final BetaIntRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaIntRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaIntRefTranlocal)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
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

        if(attached != null && attached.owner == ref){
            BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            throw abortOnTooSmallSize(2);
        }
    }


    @Override
    public final  BetaBooleanRefTranlocal openForRead(final BetaBooleanRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaBooleanRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaBooleanRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final  BetaBooleanRefTranlocal openForWrite(
        final BetaBooleanRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaBooleanRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaBooleanRefTranlocal tranlocal = (BetaBooleanRefTranlocal)attached;

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
    public final  BetaBooleanRefTranlocal openForConstruction(
        final BetaBooleanRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaBooleanRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaBooleanRefTranlocal)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
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

        if(attached != null && attached.owner == ref){
            BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            throw abortOnTooSmallSize(2);
        }
    }


    @Override
    public final  BetaDoubleRefTranlocal openForRead(final BetaDoubleRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaDoubleRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaDoubleRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final  BetaDoubleRefTranlocal openForWrite(
        final BetaDoubleRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaDoubleRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaDoubleRefTranlocal tranlocal = (BetaDoubleRefTranlocal)attached;

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
    public final  BetaDoubleRefTranlocal openForConstruction(
        final BetaDoubleRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaDoubleRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaDoubleRefTranlocal)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
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

        if(attached != null && attached.owner == ref){
            BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)attached;
            tranlocal.openForRead(config.readLockMode);
            return tranlocal.value;
        }

        if(config.trackReads || config.isolationLevel!=IsolationLevel.ReadCommitted){
            throw new TodoException();
        }else{
            throw abortOnTooSmallSize(2);
        }
    }


    @Override
    public final  BetaLongRefTranlocal openForRead(final BetaLongRef ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaLongRefTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaLongRefTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final  BetaLongRefTranlocal openForWrite(
        final BetaLongRef ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaLongRefTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal)attached;

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
    public final  BetaLongRefTranlocal openForConstruction(
        final BetaLongRef ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaLongRefTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaLongRefTranlocal)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
     }


    @Override
    public final  BetaTranlocal openForRead(final BetaTransactionalObject ref,int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForRead(ref);
        }

        if (ref == null) {
            return null;
        }

        lockMode = lockMode>=config.readLockMode ? lockMode : config.readLockMode;

        if(attached == null){
            //the transaction has no previous attached references.

            BetaTranlocal tranlocal = pool.take(ref);
            if (!ref.___load(config.spinCount, this, lockMode, tranlocal)) {
                pool.put(tranlocal);
                throw abortOnReadConflict();
            }

            tranlocal.tx = this;
            tranlocal.setStatus(STATUS_READONLY);

            if(lockMode != LOCKMODE_NONE || tranlocal.hasDepartObligation() || config.trackReads){
                attached = tranlocal;
            }else{
                throw abortOnTooSmallSize(2);
            }

            return tranlocal;
        }

        //the transaction has a previous attached reference
        if(attached.owner == ref){
            //the reference is the one we are looking for.

            final BetaTranlocal tranlocal = (BetaTranlocal)attached;

            if(tranlocal.getLockMode() < lockMode
                && !ref.___tryLockAndCheckConflict(this, config.spinCount, tranlocal, lockMode == LOCKMODE_COMMIT)){
                throw abortOnReadConflict();
            }

            return tranlocal;
        }

        if(lockMode != LOCKMODE_NONE || config.trackReads){
            throw abortOnTooSmallSize(2);
        }

        BetaTranlocal tranlocal = pool.take(ref);
        tranlocal.tx = this;
        tranlocal.setStatus(STATUS_READONLY);

        if (!ref.___load(config.spinCount, this, lockMode,tranlocal)) {
            throw abortOnReadConflict();
        }

        pool.put(tranlocal);
        throw abortOnTooSmallSize(2);
    }

    @Override
    public final  BetaTranlocal openForWrite(
        final BetaTransactionalObject ref, int lockMode) {

        if (status != ACTIVE) {
            throw abortOpenForWrite(ref);
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


            BetaTranlocal tranlocal = pool.take(ref);
            if(!ref.___load(config.spinCount, this, lockMode, tranlocal)){
                pool.put(tranlocal);
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
        BetaTranlocal tranlocal = (BetaTranlocal)attached;

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
    public final  BetaTranlocal openForConstruction(
        final BetaTransactionalObject ref) {

        if (status != ACTIVE) {
           throw abortOpenForConstruction(ref);
        }

        if (ref == null) {
            throw abortOpenForConstructionWhenNullReference();
        }

        if (config.readonly) {
            throw abortOpenForConstructionWhenReadonly(ref);
        }

        BetaTranlocal tranlocal = (attached == null || attached.owner != ref)
            ? null
            : (BetaTranlocal)attached;

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
        config.needsCommute();
        abort();
        throw SpeculativeConfigurationError.INSTANCE;
     }

 
    @Override
    public BetaTranlocal get(BetaTransactionalObject object){
        return attached == null || attached.owner!= object? null: attached;
    }

    @Override
    public BetaTranlocal locate(BetaTransactionalObject owner){
        if (status != ACTIVE) {
            throw abortLocate(owner);
        }

        if(owner == null){
            throw abortLocateWhenNullReference();
        }

        return attached == null || attached.owner!= owner? null: attached;
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

        final BetaTranlocal tranlocal = attached;
        if (tranlocal != null) {
            tranlocal.owner.___abort(this, tranlocal, pool);
            attached = null;
        }

        status = ABORTED;

    }

    // ================== commit ===========================================

    @Override
    public final void commit() {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("[%s] Failed to execute BetaTransaction.commit, reason: the transaction already is aborted", config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        if(abortOnly){
            throw abortOnWriteConflict();
        }

        final BetaTranlocal tranlocal = attached;
        Listeners listeners = null;
        if(tranlocal != null){
            if(tranlocal.ignore()){
                pool.put(tranlocal);
            }else{
                final boolean needsPrepare = status == ACTIVE
                    && hasUpdates
                    && config.readLockMode != LOCKMODE_COMMIT;

                if(config.dirtyCheck){
                    if(needsPrepare && !tranlocal.prepareDirtyUpdates(pool, this, config.spinCount)){
                        throw abortOnWriteConflict();
                    } else if(!tranlocal.isReadonly() && !tranlocal.isDirty()){
                        tranlocal.calculateIsDirty();
                    }

                    listeners = tranlocal.owner.___commitDirty(tranlocal, this, pool);
                }else{
                    if(needsPrepare && !tranlocal.prepareAllUpdates(pool, this, config.spinCount)){
                        throw abortOnWriteConflict();
                    }

                    listeners = tranlocal.owner.___commitAll(tranlocal, this, pool);
                }
            }
            attached = null;
        }

        status = COMMITTED;

        if(listeners != null){
            listeners.openAll(pool);
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
                        format("[%s] Failed to execute BetaTransaction.commit, reason: the transaction already is committed",
                            config.familyName));
                default:
                    throw new IllegalStateException();
            }
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
    }

    // ============================ retry ===================

    @Override
    public final void retry() {
        if (status != ACTIVE) throw abortOnFaultyStatusOfRetry();

        if (!config.blockingAllowed) throw abortOnNoBlockingAllowed();

        if (attached == null) throw abortOnNoRetryPossible();

        listener.reset();
        final long listenerEra = listener.getEra();
        final BetaTransactionalObject owner = attached.owner;

        final boolean noRegistration =
            owner.___registerChangeListener(listener, attached, pool, listenerEra) == REGISTRATION_NONE;
        owner.___abort(this, attached, pool);
        attached = null;
        status = ABORTED;

        if(noRegistration) throw abortOnNoRetryPossible();

        throw Retry.INSTANCE;
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
        return true;
    }

    @Override
    public void hardReset(){
        if (status == ACTIVE || status == PREPARED) {
            if(attached!=null){
                attached.owner.___abort(this, attached, pool);
            }
        }

        hasUpdates = false;
        status = ACTIVE;
        abortOnly = false;
        remainingTimeoutNs = config.timeoutNs;
        attempt = 1;
    }

    // ================== orelse ============================

}

