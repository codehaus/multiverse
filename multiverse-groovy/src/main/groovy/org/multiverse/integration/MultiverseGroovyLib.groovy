package org.multiverse.integration

import org.multiverse.api.Transaction
import org.multiverse.api.TransactionFactory
import org.multiverse.templates.TransactionTemplate
import org.multiverse.api.GlobalStmInstance
import org.multiverse.templates.OrElseTemplate
import static org.multiverse.api.ThreadLocalTransaction.*
import static org.multiverse.api.StmUtils.*;

public class MultiverseGroovyLibrary {


  public static void atomic(Map configuration, Closure block) {
    TransactionFactory txFactory = build_transaction_factory_with(configuration)

    new TransactionTemplate(txFactory) {
      Object execute(Transaction transaction) {
        block.call()
        return null
      }

    }.execute()

  }

  public static void atomic(Closure block) {
    atomic([:], block)
  }

  public static void orElseTransaction(Map configuration, Closure either, Closure or) {
    TransactionFactory txFactory = build_transaction_factory_with(configuration)
    Transaction transaction = txFactory.create()

    setThreadLocalTransaction(transaction)

    new OrElseTemplate(transaction) {
      Object either(Transaction tx) {
        either.call()
        return null;
      }

      Object orelse(Transaction tx) {
        or.call()
        return null;
      }
    }.execute()
  }

  private static TransactionFactory build_transaction_factory_with(Map configuration) {
    boolean readonly = configuration['readonly'] ?: false
    boolean trackreads = configuration['trackreads'] ?: false
    int maxRetries = configuration['maxRetries'] ?: 1000
    boolean explicitRetryAllowed = configuration['explicitRetryAllowed'] ?: false
    boolean allowWriteSkew = configuration['allowWriteSkew'] ?: true
    boolean interruptible = configuration['interruptible'] ?: true
    int timeoutInNanoSeconds = configuration['timeoutInNanoSeconds'] ?: 1000

    def globalStm = GlobalStmInstance.getGlobalStmInstance()
    def transactionFactoryBuilder = globalStm.getTransactionFactoryBuilder()

    TransactionFactory txFactory = transactionFactoryBuilder
            .setReadonly(readonly)
            .setReadTrackingEnabled(trackreads)
            .setExplicitRetryAllowed(explicitRetryAllowed)
            .setMaxRetries(maxRetries)
            .setTimeoutNs(timeoutInNanoSeconds)
            .setWriteSkewAllowed(allowWriteSkew)
            .setInterruptible(interruptible)
            .build()

    return txFactory
  }

}