package org.multiverse.integration

import org.multiverse.api.Transaction
import org.multiverse.api.TransactionFactory
import org.multiverse.templates.TransactionTemplate
import org.multiverse.api.GlobalStmInstance

public class MultiverseGroovyLibrary {

  public static void atomic(Map args, Closure block) {
    boolean readonly = args['readonly'] ?: false
    boolean trackreads = args['trackreads'] ?: false
    boolean speculativeConfigurationEnabled = args['speculativeConfigurationEnabled'] ?:false
    boolean explicitRetryAllowed = args['explicitRetryAllowed'] ?: false
    int maxRetries = args['maxRetries'] ?: 2

    def globalStm = GlobalStmInstance.getGlobalStmInstance()
    def transactionFactoryBuilder = globalStm.getTransactionFactoryBuilder()



    TransactionFactory txFactory = transactionFactoryBuilder
            .setReadonly(readonly)
            .setReadTrackingEnabled(trackreads)
            .setSpeculativeConfigurationEnabled(speculativeConfigurationEnabled)
            .setExplicitRetryAllowed(explicitRetryAllowed)
            .setMaxRetries(maxRetries).build()

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
}