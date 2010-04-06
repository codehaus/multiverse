package ${package};

import org.multiverse.annotations.TransactionalMethod;

/**
 * A Service responsible for transferring money from one acount to another.
 *
 * This service has 2 different transfer methods:
 * <ol>
 * <li>transactionalTransfer: that is transactional and therefor not subject to isolation problems are partial writes 
 * (failure atomicity) </li>
 * <li>nonTransactionalTransfer: that is not transactional so subject to isolation and partial writes</li>
 * </ol>
 * The goal of these 2 different methods is to show what can go wrong if something is not transactional.
 */
public class TransferService {

    @TransactionalMethod
    public void transactionalTransfer(Account from, Account to, int sum){
        from.set(from.get() - sum);
        to.set(to.get() + sum);
    }

    public void nonTransactionalTransfer(Account from, Account to, int sum){
        from.set(from.get() - sum);
        to.set(to.get() + sum);
    }
}
