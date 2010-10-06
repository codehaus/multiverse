package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.transactions.BetaTransaction;

/**
 * The transactional object. Atm it is just a reference for an int, more complex stuff will be added again
 * once this project leaves the prototype stage.
 * <p/>
 * remember:
 * it could be that the lock is acquired, but the lockOwner has not been set yet.
 *
 * The whole idea of code generation is that once you are inside a concrete class,
 * polymorphism is needed anymore.
 *
 * This class is generated.
 *
 * @author Peter Veentjer
 */
public abstract class AbstractBetaTransactionalObject
    extends VeryAbstractBetaTransactionalObject
{


     /**
     * Creates a uncommitted AbstractBetaTransactionalObject that should be attached to the transaction (this
     * is not done)
     *
     * @param tx the transaction this AbstractBetaTransactionalObject should be attached to.
     * @throws NullPointerException if tx is null.
     */
    public AbstractBetaTransactionalObject(BetaTransaction tx){
        super(tx.getConfiguration().stm);
        ___tryLockAndArrive(0, true);
        this.___lockOwner = tx;
    }


   @Override
    public final int ___getClassIndex(){
        return -1;
    }


   

    @Override
    public final void ___abort(
        final BetaTransaction transaction,
        final Tranlocal tranlocal,
        final BetaObjectPool pool) {

        if(tranlocal.isLockOwner){
            ___lockOwner = null;

            if(!tranlocal.isConstructing){
                //depart and release the lock. This call is able to deal with readbiased and normal reads.
                ___departAfterFailureAndUnlock();
            }
        }else{
            if(tranlocal.hasDepartObligation){
                ___departAfterFailure();
            }
        }

        pool.put((Tranlocal)tranlocal);        
    }

}
