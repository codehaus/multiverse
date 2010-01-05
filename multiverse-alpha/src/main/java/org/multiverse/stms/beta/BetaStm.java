package org.multiverse.stms.beta;

import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.utils.TodoException;
import org.multiverse.utils.clock.Clock;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class BetaStm implements Stm {

    private Clock clock = new StrictClock();

    public BetaStm() {
        clock.tick();
    }

    @Override
    public TransactionFactoryBuilder getTransactionFactoryBuilder() {
        throw new TodoException();
    }

    @Override
    public long getVersion() {
        return clock.getVersion();
    }


    /*
   class UpdateTransaction extends AbstractTransaction implements BetaTransaction {

       private final Map<BetaAtomicObject, BetaTranlocal> attached =
               new HashMap<BetaAtomicObject, BetaTranlocal>();

       public UpdateTransaction(AbstractTransactionConfig dependencies, String familyName) {
           super(dependencies, null);

           init();
       }

       @Override
       protected void doInit() {
           attached.clear();
       }

       @Override
       public BetaTranlocal load(BetaAtomicObject atomicObject) {
           if (getStatus() != TransactionStatus.active) {
               throw new DeadTransactionException();
           }

           if (atomicObject == null) {
               return null;
           }

           BetaTranlocal tranlocal = attached.get(atomicObject);
           if (tranlocal != null) {
               return tranlocal;
           }

           BetaTranlocal readonlyTranlocal = atomicObject.loadReadonly(getReadVersion());

           BetaTranlocal updatableTranlocal;
           if (readonlyTranlocal == null) {
               updatableTranlocal = atomicObject.createInitialTranlocal();
           } else {
               updatableTranlocal = readonlyTranlocal.cloneForUpdate();
           }

           attached.put(atomicObject, updatableTranlocal);
           return updatableTranlocal;
       }

       @Override
       protected long onCommit() {
           try {
               if (!acquireLocks()) {
                   throw new FailedToObtainCommitLocksException();
               }

               if (hasWriteConflicts()) {
                   throw new WriteConflictException();
               }

               long commitVersion = clock.tick();
               storeAll(commitVersion);
               return commitVersion;
           } finally {
               releaseLocks();
           }
       }

       private boolean hasWriteConflicts() {
           for (Map.Entry<BetaAtomicObject, BetaTranlocal> entry : attached.entrySet()) {
               BetaTranlocal tranlocal = entry.getValue();
               if(tranlocal instanceof DelayedBetaTranlocal){
                   ((DelayedBetaTranlocal)tranlocal).fixate(this);
               }

               if (tranlocal.getStatus() == BetaTranlocalStatus.conflict) {
                   return true;
               }
           }

           return false;
       }

       private void storeAll(long commitVersion) {
           for (Map.Entry<BetaAtomicObject, BetaTranlocal> entry : attached.entrySet()) {
               BetaAtomicObject atomicObject = entry.getKey();
               BetaTranlocal tranlocal = entry.getValue();
               tranlocal.___version = commitVersion;
               atomicObject.write(tranlocal);
           }
       }

       private boolean acquireLocks() {
           for (BetaAtomicObject atomicObject : attached.keySet()) {
               if (!atomicObject.lock(this)) {
                   return false;
               }
           }

           return true;
       }

       private void releaseLocks() {
           for (BetaAtomicObject atomicObject : attached.keySet()) {
               atomicObject.unlock(this);
           }
       }
   } */
}
