#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static java.lang.String.format;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

/**
 * An Account is responsible for storing a balance for a bank customer.
 */
@TransactionalObject
public final class Account{
    private int balance;

    @TransactionalMethod(readonly = true)
    public int get() {
        return balance;
    }

    public void set(int newBalance) {
        if(newBalance < 0){
             throw new IllegalArgumentException("An account can't have a negative balance");
        }
        this.balance = newBalance;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("Account(balance=%s)", balance);
    }
}