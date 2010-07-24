package org.multiverse.durability.account;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.durability.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Peter Veentjer
 */
public class AccountTest {
    private SimpleStorage storage;

    @Before
    public void setUp() {
        storage = new SimpleStorage();
        storage.clear();
        storage.register(Account.class, new AccountSerialized());
        storage.register(Customer.class, new OwnerSerializer());
    }

    @Test
    public void test() {
        Account root = new Account();

        System.out.println("root.storageId:"+root.getStorageId());

        Customer customer = new Customer();
        customer.active = new CustomerTranlocal(customer);
        customer.active.age = 10;
        customer.active.name = "peter";

        root.active = new AccountTranlocal(root);

        UnitOfWork transaction = storage.startUnitOfWork();
        transaction.addChange(root.active);
        transaction.addRoot(root);
        transaction.commit();

        root.active.customer = customer;

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addChange(root.active);
        unitOfWork.addChange(customer.active);
        unitOfWork.commit();
    }

    @Test
    public void testAddRoot() {
        Account account = new Account();
        Customer owner = new Customer();
        AccountTranlocal accountTranlocal = new AccountTranlocal(account);
        accountTranlocal.balance = 100;
        accountTranlocal.customer = owner;

        UnitOfWork unitOfWork = storage.startUnitOfWork();
        unitOfWork.addRoot(owner);
        unitOfWork.addChange(account.active);
        unitOfWork.commit();

        storage.clearEntities();

        Account found = (Account) storage.loadDurableObject(account.getStorageId());
        AccountTranlocal tranlocal = (AccountTranlocal) storage.loadState(account.getStorageId());
        assertNotNull(tranlocal);
        assertEquals(accountTranlocal.balance, tranlocal.balance);
    }

    public class OwnerSerializer implements DurableObjectSerializer {

        @Override
        public byte[] serialize(DurableState state) {
            CustomerTranlocal ownerTranlocal = (CustomerTranlocal) state;
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", ownerTranlocal.name);
            map.put("age", "" + ownerTranlocal.age);
            return SerializeUtils.serialize(map);
        }

        @Override
        public CustomerTranlocal deserializeState(DurableObject o, byte[] content,DurableObjectLoader loader) {
            Customer owner = (Customer) o;
            Map<String, String> map = SerializeUtils.deserializeMap(content);
            CustomerTranlocal tranlocal = new CustomerTranlocal(owner);
            tranlocal.age = Integer.parseInt(map.get("age"));
            tranlocal.name = map.get("name");
            return tranlocal;
        }

        @Override
        public DurableObject deserializeObject(String id, byte[] content) {
            Customer owner = new Customer();
            owner.setStorageId(id);
            return owner;
        }
    }

    public class AccountSerialized implements DurableObjectSerializer {

        @Override
        public byte[] serialize(DurableState state) {
            AccountTranlocal accountTranlocal = (AccountTranlocal) state;
            Map<String, String> map = new HashMap<String, String>();

            if (accountTranlocal.customer == null) {
                map.put("owner", null);
            } else {
                map.put("owner", "" + accountTranlocal.customer.getStorageId());
            }

            map.put("balance", "" + accountTranlocal.balance);
            return SerializeUtils.serialize(map);
        }

        @Override
        public DurableState deserializeState(DurableObject o, byte[] content, DurableObjectLoader loader) {
            Map<String, String> map = SerializeUtils.deserializeMap(content);

            Account owner = (Account) o;
            AccountTranlocal active = new AccountTranlocal(owner);
            active.balance = Integer.parseInt(map.get("balance"));

            String ownerId = map.get("owner");
            if (ownerId != null) {
                //todo
            }

            return active;
        }

        @Override
        public DurableObject deserializeObject(String id, byte[] content) {
            Account account = new Account();
            account.setStorageId(id);
            return account;
        }
    }
}
