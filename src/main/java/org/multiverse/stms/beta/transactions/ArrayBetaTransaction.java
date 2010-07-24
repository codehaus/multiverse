package org.multiverse.stms.beta.transactions;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.*;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.conflictcounters.LocalConflictCounter;
import org.multiverse.stms.beta.refs.Tranlocal;
import static org.multiverse.stms.beta.ThreadLocalObjectPool.*;

import java.util.concurrent.atomic.AtomicLong;
import static java.lang.String.format;

public final class ArrayBetaTransaction extends AbstractBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private final LocalConflictCounter localConflictCounter;
    private final Tranlocal[] array;
    private BetaTransactionConfig config;

    private int firstFreeIndex = 0;
    private boolean needsRealClose = false;

    //   protected boolean hasWrites = false;

    public ArrayBetaTransaction(final BetaStm stm, final int length) {
        this(new BetaTransactionConfig(stm), length);
    }

    public ArrayBetaTransaction(final BetaTransactionConfig config, final int length) {
        this.config = config;
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[length];
    }

    @Override
    public final BetaTransactionConfig getConfig(){
        return config;
    }

    public final LocalConflictCounter getLocalConflictCounter() {
        return localConflictCounter;
    }

    @Override
    public void start(){
        start(getThreadLocalObjectPool());
    }

    @Override
    public void start(final ObjectPool pool){
        if(status != NEW){
            switch(status){
                case ACTIVE:
                    //it can't do harm to start an already started transaction
                    return;
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't start already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't start already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't start already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override
    public <E> RefTranlocal<E> openForRead(
        final Ref<E> ref, boolean lock, final ObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't read from already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't read from already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't read from already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index >= 0){
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];

            //lock it if needed: todo take care of the constructed object.
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }
            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        //only if the size currently is 1, we can do the localConflictCounter reset. Only from this point
        //we are interested in conflicts. Not before.
        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //none is found in this transaction, lets load it.
        RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read == null) {
            //todo: abort can't deal with null
            //a read never can work on a null.
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock){
            needsRealClose = true;            
        }

        array[firstFreeIndex] = read;
        firstFreeIndex++;
        return read;
    }

    @Override
    public <E> RefTranlocal<E> openForWrite(
        final Ref<E>  ref, boolean lock, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write to already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write to already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write to already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index >= 0){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        RefTranlocal<E> result;
        if(read == null || read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final Ref<E> ref, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write fresh object on already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }


        int index = indexOf(ref);
        if(index >= 0){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if(result.isCommitted){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if(result.read!=null){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }
    
        //it was not previously attached to this transaction

        if(ref.unsafeLoad() != null){
            //todo: improved exception
            throw new IllegalArgumentException();
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //open the tranlocal for writing.
        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }        
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public  IntRefTranlocal openForRead(
        final IntRef ref, boolean lock, final ObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't read from already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't read from already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't read from already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index >= 0){
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];

            //lock it if needed: todo take care of the constructed object.
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }
            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        //only if the size currently is 1, we can do the localConflictCounter reset. Only from this point
        //we are interested in conflicts. Not before.
        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //none is found in this transaction, lets load it.
        IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read == null) {
            //todo: abort can't deal with null
            //a read never can work on a null.
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock){
            needsRealClose = true;            
        }

        array[firstFreeIndex] = read;
        firstFreeIndex++;
        return read;
    }

    @Override
    public  IntRefTranlocal openForWrite(
        final IntRef  ref, boolean lock, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write to already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write to already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write to already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index >= 0){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        IntRefTranlocal result;
        if(read == null || read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final IntRef ref, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write fresh object on already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }


        int index = indexOf(ref);
        if(index >= 0){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if(result.isCommitted){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if(result.read!=null){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }
    
        //it was not previously attached to this transaction

        if(ref.unsafeLoad() != null){
            //todo: improved exception
            throw new IllegalArgumentException();
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //open the tranlocal for writing.
        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }        
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public  LongRefTranlocal openForRead(
        final LongRef ref, boolean lock, final ObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't read from already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't read from already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't read from already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index >= 0){
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];

            //lock it if needed: todo take care of the constructed object.
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }
            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        //only if the size currently is 1, we can do the localConflictCounter reset. Only from this point
        //we are interested in conflicts. Not before.
        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //none is found in this transaction, lets load it.
        LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read == null) {
            //todo: abort can't deal with null
            //a read never can work on a null.
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock){
            needsRealClose = true;            
        }

        array[firstFreeIndex] = read;
        firstFreeIndex++;
        return read;
    }

    @Override
    public  LongRefTranlocal openForWrite(
        final LongRef  ref, boolean lock, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write to already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write to already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write to already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index >= 0){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        LongRefTranlocal result;
        if(read == null || read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final LongRef ref, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write fresh object on already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }


        int index = indexOf(ref);
        if(index >= 0){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if(result.isCommitted){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if(result.read!=null){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }
    
        //it was not previously attached to this transaction

        if(ref.unsafeLoad() != null){
            //todo: improved exception
            throw new IllegalArgumentException();
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //open the tranlocal for writing.
        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }        
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public  DoubleRefTranlocal openForRead(
        final DoubleRef ref, boolean lock, final ObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't read from already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't read from already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't read from already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index >= 0){
            //we are lucky, at already is attached to the session
            DoubleRefTranlocal found = (DoubleRefTranlocal)array[index];

            //lock it if needed: todo take care of the constructed object.
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }
            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        //only if the size currently is 1, we can do the localConflictCounter reset. Only from this point
        //we are interested in conflicts. Not before.
        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //none is found in this transaction, lets load it.
        DoubleRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read == null) {
            //todo: abort can't deal with null
            //a read never can work on a null.
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock){
            needsRealClose = true;            
        }

        array[firstFreeIndex] = read;
        firstFreeIndex++;
        return read;
    }

    @Override
    public  DoubleRefTranlocal openForWrite(
        final DoubleRef  ref, boolean lock, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write to already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write to already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write to already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index >= 0){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final DoubleRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        DoubleRefTranlocal result;
        if(read == null || read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final DoubleRef ref, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write fresh object on already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }


        int index = indexOf(ref);
        if(index >= 0){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];

            if(result.isCommitted){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if(result.read!=null){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }
    
        //it was not previously attached to this transaction

        if(ref.unsafeLoad() != null){
            //todo: improved exception
            throw new IllegalArgumentException();
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //open the tranlocal for writing.
        DoubleRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }        
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public  Tranlocal openForRead(
        final BetaTransactionalObject ref, boolean lock, final ObjectPool pool) {

        //make sure that the state is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't read from already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't read from already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't read from already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        //a read on a null ref, always returns a null tranlocal.
        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;

        final int index = indexOf(ref);

        if(index >= 0){
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];

            //lock it if needed: todo take care of the constructed object.
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[index] = found;
            }
            return found;
        }

        //check if the size is not exceeded.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        //only if the size currently is 1, we can do the localConflictCounter reset. Only from this point
        //we are interested in conflicts. Not before.
        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //none is found in this transaction, lets load it.
        Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        if (read == null) {
            //todo: abort can't deal with null
            //a read never can work on a null.
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //make sure that there are no conflicts.
        if (hasReadConflict()) {
            ref.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        if(lock){
            needsRealClose = true;            
        }

        array[firstFreeIndex] = read;
        firstFreeIndex++;
        return read;
    }

    @Override
    public  Tranlocal openForWrite(
        final BetaTransactionalObject  ref, boolean lock, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write to already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write to already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write to already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        lock = lock || config.lockWrites;

        final int index = indexOf(ref);
        if(index >= 0){
            Tranlocal result = (Tranlocal)array[index];

            //an optimization that shifts the read index to the front, so it can be access faster the next time.
            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //and open it for write if needed.
            if (result.isCommitted) {
                result = result.openForWrite(pool);
                array[0] = result;
            }
            return result;
        }

        //it was not previously attached to this transaction

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        Tranlocal result;
        if(read == null || read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result = read.openForWrite(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref, final ObjectPool pool) {

        //check if the status of the transaction is correct.
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't write fresh object on already prepared transaction '%s'",config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't write fresh object on already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'",config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }


        int index = indexOf(ref);
        if(index >= 0){
            Tranlocal result = (Tranlocal)array[index];

            if(result.isCommitted){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if(result.read!=null){
                //todo: improve exception
                throw new IllegalArgumentException();
            }

            if (index > 0) {
                array[index] = array[0];
                array[0] = result;
            }

            return result;
        }
    
        //it was not previously attached to this transaction

        if(ref.unsafeLoad() != null){
            //todo: improved exception
            throw new IllegalArgumentException();
        }

        //make sure that the transaction doesn't overflow.
        if (firstFreeIndex == array.length) {
            throw abortOnTooSmallSize(pool);
        }

        if(firstFreeIndex == 0){
            localConflictCounter.reset();
        }

        //open the tranlocal for writing.
        Tranlocal result = ref.openForConstruction(pool);
        array[firstFreeIndex] = result;
        firstFreeIndex++;
        return result;
    }
 
    /**
     * Finds the index of the tranlocal that has the ref as owner. Return -1 if not found.
     *
     * @param owner the owner of the tranlocal to look for.
     * @return the index of the tranlocal, or -1 if not found.
     */
    private int indexOf(BetaTransactionalObject owner){
        assert owner!=null;

        for(int k=0; k<firstFreeIndex; k++){
            Tranlocal tranlocal = array[k];
            if(tranlocal.owner == owner){
                return k;
            }
        }

        return -1;
    }

    private boolean hasReadConflict() {
        if (firstFreeIndex == 0 || config.lockReads) {
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal.owner.hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================= abort ===================================

    @Override
    public void abort() {
        abort(getThreadLocalObjectPool());
    }

    @Override
    public final void abort(final ObjectPool pool) {
        switch (status) {
            case ACTIVE:
                //fall through
            case PREPARED:
                final int _firstFreeIndex = firstFreeIndex;

                for (int k = 0; k < _firstFreeIndex; k++) {
                    Tranlocal tranlocal = array[k];
                    //abort could be expensive.
                    tranlocal.owner.abort(this, tranlocal, pool);
                }
                status = ABORTED;
                break;
            case ABORTED:
                break;
            case COMMITTED:
                throw new DeadTransactionException(
                    format("Can't abort already committed transaction '%s'",config.familyName));
            default:
                throw new IllegalStateException();
        }
    }

    // ================================== commit =================================

    @Override
    public void commit() {
         commit(getThreadLocalObjectPool());
    }

    @Override
    public final void commit(final ObjectPool pool) {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't commit already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        Listeners[] listeners = null;

        if (firstFreeIndex > 0) {
            if(config.dirtyCheck){
                if(status == ACTIVE && !doPrepareDirty()){
                    throw abortOnWriteConflict(pool);
                }

                listeners = commitDirty(pool);
            }else{
                if(status == ACTIVE && !doPrepareAll()){
                     throw abortOnWriteConflict(pool);
                }

                listeners = commitAll(pool);
            }
        }

        status = COMMITTED;

        if(listeners != null){
            openListeners(listeners, pool);
        }

        if(permanentListeners != null){
            notifyListeners(permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    private Listeners[] commitAll(final ObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            Listeners listeners = tranlocal.owner.commitAll(tranlocal, this, pool, config.globalConflictCounter);

            if(listeners != null){
                //todo: this listeners array could have been pooled
                if(listenersArray == null){
                    listenersArray = new Listeners[firstFreeIndex-k];
                }
                listenersArray[storeIndex]=listeners;
                storeIndex++;
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty(final ObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];
            Listeners listeners = tranlocal.owner.commitDirty(tranlocal, this, pool, config.globalConflictCounter);
            if(listeners != null){
                //todo: this listeners array could have been pooled
                if(listenersArray == null){
                    listenersArray = new Listeners[firstFreeIndex-k];
                }
                listenersArray[storeIndex]=listeners;
                storeIndex++;
            }
        }

        return listenersArray;
    }

    // ========================= prepare ================================

    @Override
    public void prepare() {
        prepare(getThreadLocalObjectPool());
    }

    @Override
    public void prepare(ObjectPool pool) {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                     return;
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(firstFreeIndex > 0){
           if(config.dirtyCheck){
                if(!doPrepareDirty()){
                    throw abortOnWriteConflict(pool);
                }
            }else{
                if(!doPrepareAll()){
                    throw abortOnWriteConflict(pool);
                }
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareAll() {
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (!tranlocal.isCommitted) {
                if(!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        for (int k = 0; k < firstFreeIndex; k++) {
            Tranlocal tranlocal = array[k];

            if (!tranlocal.isCommitted && tranlocal.calculateIsDirty()) {
                if(!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    // ============================== registerChangeListenerAndAbort ========================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener){
        registerChangeListenerAndAbort(listener, getThreadLocalObjectPool());
    }

    @Override
    public void registerChangeListenerAndAbort(final Latch listener, final ObjectPool pool) {
        if (status != ACTIVE) {
            switch (status) {
                case PREPARED:
                    throw new PreparedTransactionException(
                        format("Can't block on already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't block on already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't block on already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if(!config.explicitRetryAllowed){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since it explicitly is configured as non blockable",config.familyName));
        }

        if( firstFreeIndex == 0){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
        }

        final long listenerEra = listener.getEra();

        boolean register = true;
        for(int k=0; k < firstFreeIndex; k++){

            final Tranlocal tranlocal = array[k];
            final BetaTransactionalObject owner = tranlocal.owner;

            if(register){
                if(!owner.registerChangeListener(listener, tranlocal, pool, listenerEra)){
                    register = false;
                }
            }

            owner.abort(this, tranlocal, pool);
        }

        status = ABORTED;
    }

    // ==================== reset ==============================

    @Override
    public void reset(){
        reset(getThreadLocalObjectPool());
    }

    @Override
    public final void reset(final ObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }
        normalListeners = null;
        status = ACTIVE;
        firstFreeIndex = 0;
        needsRealClose = false;
    }

    // ==================== init =============================

    @Override
    public void init(BetaTransactionConfig transactionConfig){
        init(transactionConfig, getThreadLocalObjectPool());
    }

    @Override
    public void init(BetaTransactionConfig transactionConfig, ObjectPool pool){
        if(transactionConfig == null){
            throw new NullPointerException();
        }
                       
        if(status == ACTIVE || status == PREPARED){
            abort(pool);
        }

        this.config = transactionConfig;
        this.attempt = 1;
        this.permanentListeners = null;
        reset();
    }

    // ================== orelse ============================

    @Override
    public final void startEitherBranch(){
        startEitherBranch(getThreadLocalObjectPool());
    }

    @Override
    public final void startEitherBranch(ObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void endEitherBranch(){
        endEitherBranch(getThreadLocalObjectPool());
    }

    @Override
    public final void endEitherBranch(ObjectPool pool){
        throw new TodoException();
    }

    @Override
    public final void startOrElseBranch(){
        startOrElseBranch(getThreadLocalObjectPool());
    }

    @Override
    public final void startOrElseBranch(ObjectPool pool){
        throw new TodoException();
    }
}
