package org.multiverse.durability.account;

import org.multiverse.durability.DurableObject;

import java.util.UUID;

/**
 * @author Peter Veentjer
 */
public class Customer implements DurableObject {

    private String storageId;
    public CustomerTranlocal active;

    @Override
    public String getStorageId() {
        if(storageId == null){
            storageId = UUID.randomUUID().toString();
        }

        return storageId;
    }

    @Override
    public void setStorageId(String id) {
        this.storageId = id;
    }

}
