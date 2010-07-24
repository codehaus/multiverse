package org.multiverse.stms.beta.transactions;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.*;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.lifecycle.*;
import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.refs.*;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;


import static org.multiverse.stms.beta.ThreadLocalObjectPool.*;

import static java.lang.String.format;

/**
 * A BetaTransaction tailored for dealing with 1 transactional object.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public final class MonoBetaTransaction extends AbstractBetaTransaction {

    private BetaTransactionConfig config;

    private Tranlocal attached;
    private boolean needsRealClose = false;

    public MonoBetaTransaction(final BetaStm stm){
        this(new BetaTransactionConfig(stm));
    }

    public MonoBetaTransaction(final BetaTransactionConfig config) {
        this.config = config;
    }

    @Override
    public final BetaTransactionConfig getConfig(){
        return config;
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
        status = ACTIVE;
    }


    @Override
    public final <E> RefTranlocal openForRead(final Ref<E> ref,  boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        //if(status == NEW){
        //     status = ACTIVE;
        //}

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;


        RefTranlocal<E> found = attached != null && attached.owner == ref?(RefTranlocal<E>)attached: null;

        if (found != null) {
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }

                if(!needsRealClose){
                    needsRealClose = true;
                }
            }else if(!found.isPermanent){
                if(!needsRealClose){
                    needsRealClose = true;
                }
            }

            return found;
        }

        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        RefTranlocal<E> read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        //if it was null, abort. OpenForRead can't deal with refs that have not been initialized.
        if (read == null) {
            throw abortOnReadConflict(pool);
        }

        //if it was locked, lets abort.
        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //if it is locked, or the read is non permanent, we need to close the transaction
        if(lock || !read.isPermanent){
            needsRealClose = true;
        }

        attached = read;
        return read;
    }

    @Override
    public final <E> RefTranlocal<E> openForWrite(final Ref<E> ref, boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        if(status == NEW){
             status = ACTIVE;
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        lock = lock || config.lockWrites;

        needsRealClose = true;

        RefTranlocal<E> result = (attached == null || attached.owner != ref) ? null : (RefTranlocal<E>)attached;

        if (result != null) {
            //there already is a previous attached tranlocal for the ref

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //it if already is opened for writing, lets return it.
            if (!result.isCommitted) {
                return result;
            }

            //if it is committed, it should be opened for writing.
            RefTranlocal<E> read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new RefTranlocal<E>(ref);
            }
            result.value = read.value;
            result.read = read;
            attached = result;
            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        //load the current tranlocal
        RefTranlocal<E> read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            //we could not load a read because it was locked.
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }

        result.read = read;
        result.value = read.value;
        attached = result;
        return result;
    }

    @Override
    public final <E> RefTranlocal<E> openForConstruction(final Ref<E> ref, final ObjectPool pool) {
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

        RefTranlocal<E> result = (attached == null || attached.owner != ref) ? null : (RefTranlocal<E>)attached;

        if(result != null){
            //a
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

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        result =  pool.take(ref);
        if(result == null){
            result = new RefTranlocal<E>(ref);
        }
        attached = result;
        return result;
    }


    @Override
    public final  IntRefTranlocal openForRead(final IntRef ref,  boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        //if(status == NEW){
        //     status = ACTIVE;
        //}

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;


        IntRefTranlocal found = attached != null && attached.owner == ref?(IntRefTranlocal)attached: null;

        if (found != null) {
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }

                if(!needsRealClose){
                    needsRealClose = true;
                }
            }else if(!found.isPermanent){
                if(!needsRealClose){
                    needsRealClose = true;
                }
            }

            return found;
        }

        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        IntRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        //if it was null, abort. OpenForRead can't deal with refs that have not been initialized.
        if (read == null) {
            throw abortOnReadConflict(pool);
        }

        //if it was locked, lets abort.
        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //if it is locked, or the read is non permanent, we need to close the transaction
        if(lock || !read.isPermanent){
            needsRealClose = true;
        }

        attached = read;
        return read;
    }

    @Override
    public final  IntRefTranlocal openForWrite(final IntRef ref, boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        if(status == NEW){
             status = ACTIVE;
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        lock = lock || config.lockWrites;

        needsRealClose = true;

        IntRefTranlocal result = (attached == null || attached.owner != ref) ? null : (IntRefTranlocal)attached;

        if (result != null) {
            //there already is a previous attached tranlocal for the ref

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //it if already is opened for writing, lets return it.
            if (!result.isCommitted) {
                return result;
            }

            //if it is committed, it should be opened for writing.
            IntRefTranlocal read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new IntRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;
            attached = result;
            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        //load the current tranlocal
        IntRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            //we could not load a read because it was locked.
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        attached = result;
        return result;
    }

    @Override
    public final  IntRefTranlocal openForConstruction(final IntRef ref, final ObjectPool pool) {
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

        IntRefTranlocal result = (attached == null || attached.owner != ref) ? null : (IntRefTranlocal)attached;

        if(result != null){
            //a
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

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        result =  pool.take(ref);
        if(result == null){
            result = new IntRefTranlocal(ref);
        }
        attached = result;
        return result;
    }


    @Override
    public final  LongRefTranlocal openForRead(final LongRef ref,  boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        //if(status == NEW){
        //     status = ACTIVE;
        //}

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;


        LongRefTranlocal found = attached != null && attached.owner == ref?(LongRefTranlocal)attached: null;

        if (found != null) {
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }

                if(!needsRealClose){
                    needsRealClose = true;
                }
            }else if(!found.isPermanent){
                if(!needsRealClose){
                    needsRealClose = true;
                }
            }

            return found;
        }

        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        LongRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        //if it was null, abort. OpenForRead can't deal with refs that have not been initialized.
        if (read == null) {
            throw abortOnReadConflict(pool);
        }

        //if it was locked, lets abort.
        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //if it is locked, or the read is non permanent, we need to close the transaction
        if(lock || !read.isPermanent){
            needsRealClose = true;
        }

        attached = read;
        return read;
    }

    @Override
    public final  LongRefTranlocal openForWrite(final LongRef ref, boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        if(status == NEW){
             status = ACTIVE;
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        lock = lock || config.lockWrites;

        needsRealClose = true;

        LongRefTranlocal result = (attached == null || attached.owner != ref) ? null : (LongRefTranlocal)attached;

        if (result != null) {
            //there already is a previous attached tranlocal for the ref

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //it if already is opened for writing, lets return it.
            if (!result.isCommitted) {
                return result;
            }

            //if it is committed, it should be opened for writing.
            LongRefTranlocal read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new LongRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;
            attached = result;
            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        //load the current tranlocal
        LongRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            //we could not load a read because it was locked.
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        attached = result;
        return result;
    }

    @Override
    public final  LongRefTranlocal openForConstruction(final LongRef ref, final ObjectPool pool) {
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

        LongRefTranlocal result = (attached == null || attached.owner != ref) ? null : (LongRefTranlocal)attached;

        if(result != null){
            //a
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

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        result =  pool.take(ref);
        if(result == null){
            result = new LongRefTranlocal(ref);
        }
        attached = result;
        return result;
    }


    @Override
    public final  DoubleRefTranlocal openForRead(final DoubleRef ref,  boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        //if(status == NEW){
        //     status = ACTIVE;
        //}

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;


        DoubleRefTranlocal found = attached != null && attached.owner == ref?(DoubleRefTranlocal)attached: null;

        if (found != null) {
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }

                if(!needsRealClose){
                    needsRealClose = true;
                }
            }else if(!found.isPermanent){
                if(!needsRealClose){
                    needsRealClose = true;
                }
            }

            return found;
        }

        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        DoubleRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        //if it was null, abort. OpenForRead can't deal with refs that have not been initialized.
        if (read == null) {
            throw abortOnReadConflict(pool);
        }

        //if it was locked, lets abort.
        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //if it is locked, or the read is non permanent, we need to close the transaction
        if(lock || !read.isPermanent){
            needsRealClose = true;
        }

        attached = read;
        return read;
    }

    @Override
    public final  DoubleRefTranlocal openForWrite(final DoubleRef ref, boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        if(status == NEW){
             status = ACTIVE;
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        lock = lock || config.lockWrites;

        needsRealClose = true;

        DoubleRefTranlocal result = (attached == null || attached.owner != ref) ? null : (DoubleRefTranlocal)attached;

        if (result != null) {
            //there already is a previous attached tranlocal for the ref

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //it if already is opened for writing, lets return it.
            if (!result.isCommitted) {
                return result;
            }

            //if it is committed, it should be opened for writing.
            DoubleRefTranlocal read = result;
            result = pool.take(ref);
            if (result == null) {
                result = new DoubleRefTranlocal(ref);
            }
            result.value = read.value;
            result.read = read;
            attached = result;
            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        //load the current tranlocal
        DoubleRefTranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            //we could not load a read because it was locked.
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }

        result.read = read;
        result.value = read.value;
        attached = result;
        return result;
    }

    @Override
    public final  DoubleRefTranlocal openForConstruction(final DoubleRef ref, final ObjectPool pool) {
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

        DoubleRefTranlocal result = (attached == null || attached.owner != ref) ? null : (DoubleRefTranlocal)attached;

        if(result != null){
            //a
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

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        result =  pool.take(ref);
        if(result == null){
            result = new DoubleRefTranlocal(ref);
        }
        attached = result;
        return result;
    }


    @Override
    public final  Tranlocal openForRead(final BetaTransactionalObject ref,  boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        //if(status == NEW){
        //     status = ACTIVE;
        //}

        if (ref == null) {
            return null;
        }

        lock = lock || config.lockReads;


        Tranlocal found = attached != null && attached.owner == ref?(Tranlocal)attached: null;

        if (found != null) {
            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, found)){
                    throw abortOnReadConflict(pool);
                }

                if(!needsRealClose){
                    needsRealClose = true;
                }
            }else if(!found.isPermanent){
                if(!needsRealClose){
                    needsRealClose = true;
                }
            }

            return found;
        }

        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        Tranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        //if it was null, abort. OpenForRead can't deal with refs that have not been initialized.
        if (read == null) {
            throw abortOnReadConflict(pool);
        }

        //if it was locked, lets abort.
        if (read.locked) {
            throw abortOnReadConflict(pool);
        }

        //if it is locked, or the read is non permanent, we need to close the transaction
        if(lock || !read.isPermanent){
            needsRealClose = true;
        }

        attached = read;
        return read;
    }

    @Override
    public final  Tranlocal openForWrite(final BetaTransactionalObject ref, boolean lock, final ObjectPool pool) {
        assert pool!=null;

        if (status > ACTIVE) {
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

        if(status == NEW){
             status = ACTIVE;
        }

        if (ref == null) {
            abort(pool);
            throw new NullPointerException();
        }

        if (config.readonly) {
            abort(pool);
            throw new ReadonlyException(format("Can't write to readonly transaction '%s'", config.familyName));
        }

        lock = lock || config.lockWrites;

        needsRealClose = true;

        Tranlocal result = (attached == null || attached.owner != ref) ? null : (Tranlocal)attached;

        if (result != null) {
            //there already is a previous attached tranlocal for the ref

            if (lock) {
                if(!ref.tryLockAndCheckConflict(this, config.spinCount, result)){
                    throw abortOnReadConflict(pool);
                }
            }

            //it if already is opened for writing, lets return it.
            if (!result.isCommitted) {
                return result;
            }

            //if it is committed, it should be opened for writing.
            result = result.openForWrite(pool);
            attached = result;
            return result;
        }

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        //load the current tranlocal
        Tranlocal read = lock? ref.lockAndLoad(config.spinCount, this):ref.load(config.spinCount);

        if(read == null){
            throw abortOnReadConflict(pool);
        }

        if (read.locked) {
            //we could not load a read because it was locked.
            throw abortOnReadConflict(pool);
        }

        //open the tranlocal for writing.
        result = read.openForWrite(pool);
        attached = result;
        return result;
    }

    @Override
    public final  Tranlocal openForConstruction(final BetaTransactionalObject ref, final ObjectPool pool) {
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

        Tranlocal result = (attached == null || attached.owner != ref) ? null : (Tranlocal)attached;

        if(result != null){
            //a
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

        //check if there is room
        if (attached != null) {
            throw abortOnTooSmallSize(pool);
        }

        if(ref.unsafeLoad()!=null){
            throw new IllegalArgumentException();
        }

        result = ref.openForConstruction(pool);
        attached = result;
        return result;
    }

 
    // ======================= abort =======================================

    @Override
    public void abort() {
        abort(getThreadLocalObjectPool());
    }

    @Override
    public final void abort(final ObjectPool pool) {
        if (status != ACTIVE && status != PREPARED) {
            switch (status) {
                case ABORTED:
                    return;
                case COMMITTED:
                    throw new DeadTransactionException(
                        format("Can't abort already aborted transaction '%s'",config.familyName));
                default:
                    throw new IllegalStateException();
            }
        }

        if (attached != null) {
            attached.owner.abort(this, attached, pool);            
        }

        status = ABORTED;
    }

    // ================== commit ===========================================

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
                        format("Can't commit already aborted transaction '%s'", config.familyName));
                case COMMITTED:
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        Listeners listeners = null;
        if (needsRealClose) {
            if(config.dirtyCheck){
                if(status == ACTIVE){
                    if(!doPrepareDirty()){
                        throw abortOnWriteConflict(pool);
                    }
                }

//                if(config.durable){
//                    config.stm.getStorage().persist(attached.owner, attached);
//                }

                  listeners = attached.owner.commitDirty(attached, this, pool, config.globalConflictCounter);
            }else{
                if(status == ACTIVE){
                    if(!doPrepareAll()){
                        throw abortOnWriteConflict(pool);
                    }
                }

                listeners = attached.owner.commitAll(attached, this, pool, config.globalConflictCounter);
            }
        }

        status = COMMITTED;

        if(listeners != null){
            openListeners(listeners,pool);
        }

        if(permanentListeners != null){
            notifyListeners(permanentListeners, TransactionLifecycleEvent.PostCommit);
        }

        if(normalListeners != null){
            notifyListeners(normalListeners, TransactionLifecycleEvent.PostCommit);
        }
    }

    // ======================= prepare ============================

    @Override
    public void prepare() {
        prepare(getThreadLocalObjectPool());
    }

    @Override
    public final void prepare(final ObjectPool pool) {
        if(status != ACTIVE){
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

        if(needsRealClose){
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

    private boolean doPrepareDirty(){
        if (!attached.isCommitted && attached.calculateIsDirty()){
            if(!attached.owner.tryLockAndCheckConflict(this, config.spinCount, attached)){
                return false;
            }
        }

        return true;
    }

    private boolean doPrepareAll(){
        if (!attached.isCommitted){
            if(!attached.owner.tryLockAndCheckConflict(this, config.spinCount, attached)){
                return false;
            }
        }

        return true;
    }

    // ============================ registerChangeListenerAndAbort ===================

    @Override
    public void registerChangeListenerAndAbort(final Latch listener){
        registerChangeListenerAndAbort(listener, getThreadLocalObjectPool());
    }

    @Override
    public final void registerChangeListenerAndAbort(final Latch listener, final ObjectPool pool) {
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

        if( attached == null){
            throw new NoRetryPossibleException(
                format("Can't block transaction '%s', since there are no tracked reads",config.familyName));
        }

        final long listenerEra = listener.getEra();
        final BetaTransactionalObject owner = attached.owner;
        owner.registerChangeListener(listener, attached, pool, listenerEra);
        owner.abort(this, attached, pool);
        status = ABORTED;
    }

    // =========================== init ================================

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

    // ========================= reset ===============================

    @Override
    public void reset(){
        reset(getThreadLocalObjectPool());
    }

    @Override
    public final void reset(final ObjectPool pool) {
        if (status == ACTIVE || status == PREPARED) {
            abort(pool);
        }
        needsRealClose = false;
        normalListeners = null;
        status = ACTIVE;
        attached = null;
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

