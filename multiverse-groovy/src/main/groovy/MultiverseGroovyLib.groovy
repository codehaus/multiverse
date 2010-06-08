import org.multiverse.api.Transaction
import org.multiverse.api.TransactionFactory
import org.multiverse.templates.TransactionTemplate
import org.multiverse.api.GlobalStmInstance

public class MultiverseGroovyLib {

  public static void atomic(Map args, Closure block) {
    boolean readonly = args['readonly']
    boolean trackreads = args['trackreads']

    def globalStm = GlobalStmInstance.getGlobalStmInstance()
    def transactionFactoryBuilder = globalStm.getTransactionFactoryBuilder()

    transactionFactoryBuilder.setReadonly(readonly).setReadTrackingEnabled(trackreads)

    TransactionFactory txFactory = transactionFactoryBuilder.build()

    new TransactionTemplate(txFactory) {
      Object execute(Transaction transaction) {
        block.call()
        return null
      }

    }.execute()
  }

}