package org.multiverse.durability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.durability.account.Account;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SimpleObjectIdentityMapTest {
    private SimpleObjectIdentityMap map;

    @Before
    public void setUp() {
        map = new SimpleObjectIdentityMap();
    }

    @Test(expected = NullPointerException.class)
    public void get_whenNull_thenNullPointerException() {
        map.get(null);
    }

    @Test
    public void getWhenNonExisting_thenNull() {
        DurableObject o = map.get("foo");
        assertNull(o);
    }

    @Test
    public void get_whenExisting() {
        DurableObject account1 = new Account();
        DurableObject account2 = new Account();

        map.putIfAbsent(account1);
        map.putIfAbsent(account2);

        DurableObject found = map.get(account1.___getStorageId());

        assertSame(found, account1);
    }

    @Test(expected = NullPointerException.class)
    public void put_whenNull_thenNullPointerException() {
        map.putIfAbsent(null);
    }

    @Test
    public void put_whenFirstTime() {
        DurableObject account = new Account();

        DurableObject result = map.putIfAbsent(account);

        assertNull(result);
        DurableObject found = map.get(account.___getStorageId());
        assertSame(found, account);
    }

    @Test
    public void put_whenAlreadyPut() {
        DurableObject account1 = new Account();
        DurableObject account2 = new Account();
        account2.___setStorageId(account1.___getStorageId());

        map.putIfAbsent(account1);
        DurableObject result = map.putIfAbsent(account2);

        assertSame(account1, result);
        assertSame(account1, map.get(account2.___getStorageId()));
    }
}
