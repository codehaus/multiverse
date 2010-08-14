package org.multiverse.durability.account;

import org.multiverse.durability.DurableObject;
import org.multiverse.durability.DurableState;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class AccountTranlocal implements DurableState {

    public Customer customer;
    public int balance;
    private Account owner;

    public AccountTranlocal(Account owner) {
        this.owner = owner;
    }

    @Override
    public DurableObject getOwner() {
        return owner;
    }

    @Override
    public Iterator<DurableObject> getReferences() {
        List<DurableObject> list = new LinkedList<DurableObject>();
        if (customer != null) {
            list.add(customer);
        }
        return list.iterator();
    }
}
