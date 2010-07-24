package org.multiverse.durability.account;

import org.multiverse.durability.DurableObject;

import java.util.UUID;

/**
 * @author Peter Veentjer
 */
public class Account implements DurableObject {

    public AccountTranlocal active;
    private String storageId = UUID.randomUUID().toString();

    @Override
    public String getStorageId() {
        return storageId;
    }

    @Override
    public void setStorageId(String id) {
        this.storageId = id;
    }

    @Override
    public String toString(){
        return storageId;
    }

}
