package org.multiverse.durability.account;

import org.multiverse.api.exceptions.TodoException;
import org.multiverse.durability.DurableObject;

import java.util.UUID;

/**
 * @author Peter Veentjer
 */
public class Customer implements DurableObject {

    private String storageId;
    public CustomerTranlocal active;

    @Override
    public String ___getStorageId() {
        if (storageId == null) {
            storageId = UUID.randomUUID().toString();
        }

        return storageId;
    }

    @Override
    public void ___setStorageId(String id) {
        this.storageId = id;
    }

    @Override
    public void ___markAsDurable() {
        throw new TodoException();
    }

    @Override
    public boolean ___isDurable() {
        throw new TodoException();
    }
}
