package org.multiverse.stms.beta.transactions;

import java.util.*;

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


/**
 * A {@link BetaTransaction} for arbitrary size transactions.
 *
 * @author Peter Veentjer.
 */
public final class ArrayTreeBetaTransaction extends AbstractBetaTransaction {

    public final static AtomicLong conflictScan = new AtomicLong();

    private LocalConflictCounter localConflictCounter;
    private BetaTransactionConfig config;
    private Tranlocal[] array;
    private int size;
    private boolean needsRealClose = false;

    public ArrayTreeBetaTransaction(BetaStm stm) {
        this(new BetaTransactionConfig(stm));
    }

    public ArrayTreeBetaTransaction(BetaTransactionConfig config) {
        this.config = config;
        this.localConflictCounter = config.globalConflictCounter.createLocalConflictCounter();
        this.array = new Tranlocal[config.maxLinearSearch];
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
                        format("Can't start already prepared transaction '%s'", config.familyName));
                case ABORTED:
                    throw new DeadTransactionException(
                        format("Can't start already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't start already committed transaction '%s'", config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        System.out.println("implement");
    }

    public int size(){
        return size;
    }

    public float getUsage(){
        return (size * 1.0f)/array.length;
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

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index>-1) {
            //we are lucky, at already is attached to the session
            RefTranlocal<E> found = (RefTranlocal<E>)array[index];

            //lock it if needed
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            return found;
        }

        if(size == 0){
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

        if(lock || !read.isPermanent || config.trackReads){
            needsRealClose = true;
            attach(ref, read, identityHashCode, pool);
            size++;
        }

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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            if (!result.isCommitted) {
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            needsRealClose = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(size == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final RefTranlocal<E> read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        RefTranlocal<E> result;
        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if(read.locked){
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
        attach(ref, result, identityHashCode, pool);
        size++;

        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(
        final Ref<E> ref, final ObjectPool pool) {
        assert pool!=null;

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

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        needsRealClose = true;
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            RefTranlocal<E> result = (RefTranlocal<E>)array[index];
            if(result.isCommitted){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            if(result.read != null){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        if(size == 0){
           localConflictCounter.reset();
        }

        RefTranlocal<E> result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
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

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index>-1) {
            //we are lucky, at already is attached to the session
            IntRefTranlocal found = (IntRefTranlocal)array[index];

            //lock it if needed
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            return found;
        }

        if(size == 0){
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

        if(lock || !read.isPermanent || config.trackReads){
            needsRealClose = true;
            attach(ref, read, identityHashCode, pool);
            size++;
        }

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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            if (!result.isCommitted) {
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            needsRealClose = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(size == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final IntRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        IntRefTranlocal result;
        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if(read.locked){
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
        attach(ref, result, identityHashCode, pool);
        size++;

        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(
        final IntRef ref, final ObjectPool pool) {
        assert pool!=null;

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

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        needsRealClose = true;
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            IntRefTranlocal result = (IntRefTranlocal)array[index];
            if(result.isCommitted){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            if(result.read != null){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        if(size == 0){
           localConflictCounter.reset();
        }

        IntRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
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

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index>-1) {
            //we are lucky, at already is attached to the session
            LongRefTranlocal found = (LongRefTranlocal)array[index];

            //lock it if needed
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            return found;
        }

        if(size == 0){
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

        if(lock || !read.isPermanent || config.trackReads){
            needsRealClose = true;
            attach(ref, read, identityHashCode, pool);
            size++;
        }

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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            if (!result.isCommitted) {
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            needsRealClose = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(size == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final LongRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        LongRefTranlocal result;
        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if(read.locked){
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
        attach(ref, result, identityHashCode, pool);
        size++;

        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(
        final LongRef ref, final ObjectPool pool) {
        assert pool!=null;

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

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        needsRealClose = true;
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            LongRefTranlocal result = (LongRefTranlocal)array[index];
            if(result.isCommitted){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            if(result.read != null){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        if(size == 0){
           localConflictCounter.reset();
        }

        LongRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
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

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index>-1) {
            //we are lucky, at already is attached to the session
            DoubleRefTranlocal found = (DoubleRefTranlocal)array[index];

            //lock it if needed
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            return found;
        }

        if(size == 0){
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

        if(lock || !read.isPermanent || config.trackReads){
            needsRealClose = true;
            attach(ref, read, identityHashCode, pool);
            size++;
        }

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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            if (!result.isCommitted) {
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            needsRealClose = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(size == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final DoubleRefTranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        DoubleRefTranlocal result;
        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if(read.locked){
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
        attach(ref, result, identityHashCode, pool);
        size++;

        return result;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(
        final DoubleRef ref, final ObjectPool pool) {
        assert pool!=null;

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

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        needsRealClose = true;
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            DoubleRefTranlocal result = (DoubleRefTranlocal)array[index];
            if(result.isCommitted){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            if(result.read != null){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        if(size == 0){
           localConflictCounter.reset();
        }

        DoubleRefTranlocal result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }
        attach(ref, result, identityHashCode, pool);
        size++;
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

        final int identityHashCode = ref.identityHashCode();
        int index = findAttachedIndex(ref, identityHashCode);
        if (index>-1) {
            //we are lucky, at already is attached to the session
            Tranlocal found = (Tranlocal)array[index];

            //lock it if needed
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount,found)){
                    throw abortOnReadConflict(pool);
                }
                needsRealClose = true;
            }

            return found;
        }

        if(size == 0){
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

        if(lock || !read.isPermanent || config.trackReads){
            needsRealClose = true;
            attach(ref, read, identityHashCode, pool);
            size++;
        }

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
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        //an openForWrite can't open a null ref.
        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        //lets find the tranlocal
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);
        lock = lock || config.lockWrites;

        if(index >- 1){
            Tranlocal result = (Tranlocal)array[index];

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            if (!result.isCommitted) {
                return result;
            }

            //it was opened for reading so we need to open it for writing.
            result = result.openForWrite(pool);
            needsRealClose = true;
            array[index]=result;
            return result;
        }

        //it was not previously attached to this transaction

        if(size == 0){
            localConflictCounter.reset();
        }

        //the tranlocal was not loaded before in this transaction, now load it.
        final Tranlocal read = lock ? ref.lockAndLoad(config.spinCount, this) : ref.load(config.spinCount);

        Tranlocal result;
        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if(read.locked){
           throw abortOnReadConflict(pool);
        }

        if (hasReadConflict()) {
            read.owner.abort(this, read, pool);
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result = read.openForWrite(pool);
        attach(ref, result, identityHashCode, pool);
        size++;

        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(
        final BetaTransactionalObject ref, final ObjectPool pool) {
        assert pool!=null;

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

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(
                format("Can't construct new object using readonly transaction '%s'",config.familyName));
        }

        needsRealClose = true;
        final int identityHashCode = ref.identityHashCode();
        final int index = findAttachedIndex(ref, identityHashCode);

        if(index >- 1){
            Tranlocal result = (Tranlocal)array[index];
            if(result.isCommitted){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            if(result.read != null){
                //todo: better exception
                throw new IllegalArgumentException();
            }

            return result;
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        if(size == 0){
           localConflictCounter.reset();
        }

        Tranlocal result = ref.openForConstruction(pool);
        attach(ref, result, identityHashCode, pool);
        size++;
        return result;
    }

 
    private int findAttachedIndex(final BetaTransactionalObject ref, final int hash){
        int jump = 0;
        boolean goLeft = true;

        do{
            int offset = goLeft?-jump:jump;
            int index = (hash + offset) % array.length;

            Tranlocal current = array[index];
            if(current == null){
                return -1;
            }

            if(current.owner == ref){
                return index;
            }

            int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0 ? 1 : jump*2;
        }while(jump < array.length);

        return -1;
    }

    private void attach(final BetaTransactionalObject ref, final Tranlocal tranlocal, final int hash, final ObjectPool pool){
        int jump = 0;
        boolean goLeft = true;

        do{
            int offset = goLeft?-jump:jump;
            int index = (hash + offset) % array.length;

            Tranlocal current = array[index];
            if(current == null){
                array[index] = tranlocal;
                return;
            }

            int currentHash = current.owner.identityHashCode();
            goLeft = currentHash > hash;
            jump = jump == 0?1:jump*2;
        }while(jump < array.length);

        expand(pool);
        attach(ref, tranlocal, hash, pool);
    }

    private void expand(final ObjectPool pool){
        Tranlocal[] oldArray = array;
        int newSize = oldArray.length*2;
        array = pool.takeTranlocalArray(newSize);
        if(array == null){
            array = new Tranlocal[newSize];
        }

        for(int k=0; k < oldArray.length; k++){
            Tranlocal tranlocal = oldArray[k];

            if(tranlocal != null){
               attach(tranlocal.owner, tranlocal, tranlocal.owner.identityHashCode(),pool);
            }
        }

        pool.putTranlocalArray(oldArray);
    }

    private boolean hasReadConflict() {
        if (size == 0 || config.lockReads) {
            return false;
        }

        if (!localConflictCounter.syncAndCheckConflict()) {
            return false;
        }

        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if (tranlocal != null && tranlocal.owner.hasReadConflict(tranlocal, this)) {
                return true;
            }
        }

        return false;
    }

    // ============================== abort ===================================================

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
    //            if(needsRealClose){
                  for (int k = 0; k < array.length; k++) {
                      Tranlocal tranlocal = array[k];
                      if(tranlocal != null){
                          tranlocal.owner.abort(this, tranlocal, pool);
                      }
  //                }
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

    // ============================== commit ==================================================

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

        Listeners[] listeners;
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
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal != null){
                Listeners listeners = tranlocal.owner.commitAll(tranlocal, this, pool, config.globalConflictCounter);

                //todo: this listeners array could have been pooled
                if(listeners != null){
                    if(listenersArray == null){
                        listenersArray = new Listeners[size];
                    }

                    listenersArray[storeIndex]=listeners;
                    storeIndex++;
                }
            }
        }

        return listenersArray;
    }

    private Listeners[] commitDirty(final ObjectPool pool) {
        Listeners[] listenersArray = null;

        int storeIndex = 0;
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];
            if(tranlocal != null){
                 Listeners listeners = tranlocal.owner.commitDirty(tranlocal, this, pool, config.globalConflictCounter);

                //todo: this listeners array could have been pooled
                if(listeners != null){
                    if(listenersArray == null){
                        listenersArray = new Listeners[size];
                    }

                    listenersArray[storeIndex]=listeners;
                    storeIndex++;
                }
            }
        }

        return listenersArray;
    }

    // ============================== prepare ==================================================

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
                        format("Can't prepare already aborted transaction '%s'",config.familyName));
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't prepare already committed transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();

            }
        }

        if(config.dirtyCheck){
            if(!doPrepareDirty()){
                throw abortOnWriteConflict(pool);
            }
        }else{
            if(!doPrepareAll()){
                throw abortOnWriteConflict(pool);
            }
        }

        status = PREPARED;
    }

    private boolean doPrepareAll() {
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal!=null && !tranlocal.isCommitted()) {
                if(!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doPrepareDirty() {
        for (int k = 0; k < array.length; k++) {
            Tranlocal tranlocal = array[k];

            if (tranlocal!=null && !tranlocal.isCommitted() && tranlocal.calculateIsDirty()) {
                if(!tranlocal.owner.tryLockAndCheckConflict(this, config.spinCount, tranlocal)){
                    return false;
                }
            }
        }

        return true;
    }

    // ============================ registerChangeListener ===============================

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

        if( size == 0){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
        }

        final long listenerEra = listener.getEra();
        boolean register = true;
        for(int k=0; k < array.length; k++){
            final Tranlocal tranlocal = array[k];

            if(tranlocal != null){
                final BetaTransactionalObject owner = tranlocal.owner;
                if(register){
                    if(!owner.registerChangeListener(listener, tranlocal, pool, listenerEra)){
                        register = false;
                    }
                }

                owner.abort(this, tranlocal, pool);
            }
        }

        status = ABORTED;
    }

    // ============================== reset ========================================

    @Override
    public void reset(){
        reset(getThreadLocalObjectPool());
    }

    @Override
    public final void reset(final ObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }

        if(array.length>config.maxLinearSearch){
            pool.putTranlocalArray(array);
            array = pool.takeTranlocalArray(config.maxLinearSearch);
            if(array == null){
                array = new Tranlocal[config.maxLinearSearch];
            }
        }else{
            Arrays.fill(array, null);
        }

        normalListeners = null;
        status = ACTIVE;
        size = 0;
        needsRealClose = false;
    }

    // ============================== init =======================================

    @Override
    public void init(BetaTransactionConfig transactionConfig){
        init(transactionConfig, getThreadLocalObjectPool());
    }

    @Override
    public void init(BetaTransactionConfig transactionConfig, ObjectPool pool){
        if(transactionConfig == null){
            throw new NullPointerException();
        }

        if(status == ACTIVE || status ==PREPARED){
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
