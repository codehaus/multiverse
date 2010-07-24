package org.multiverse.durability.account;

import org.multiverse.durability.DurableObject;
import org.multiverse.durability.DurableState;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Peter Veentjer
 */
public class CustomerTranlocal implements DurableState {

    public String name;
    public int age;
    private final Customer owner;

    public CustomerTranlocal(Customer owner){
        this.owner = owner;
    }

    @Override
    public DurableObject getOwner() {
        return owner;
    }

    @Override
    public Iterator<DurableObject> getReferences() {
        return new LinkedList<DurableObject>().iterator();
    }
}
