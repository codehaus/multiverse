package org.multiverse;

import org.multiverse.api.TransactionStatus;
import org.multiverse.api.blocking.Latch;
import org.multiverse.api.blocking.Listeners;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;
import org.multiverse.utils.Bugshaker;
import org.multiverse.utils.ThreadLocalRandom;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class TestUtils {

    public static void assertAllNull(Tranlocal[] array){
        assertNotNull(array);

        for(Tranlocal tranlocal: array){
            assertNull(tranlocal);
        }
    }

    public static void assertEqualByteArray(byte[] array1, byte[] array2){
        if(array1 == array2){
            return;
        }

        if(array1 == null){
            fail();
        }

        int length = array1.length;
        assertEquals(length, array2.length);
        for(int k=0;k<array1.length;k++){
            assertEquals(array1[k],array2[k]);
        }
    }

    public static void assertHasListeners(BetaTransactionalObject ref, Latch... listeners){
        Set<Latch> expected = new HashSet(Arrays.asList(listeners));

        Set<Latch> found = new HashSet<Latch>();
        Listeners l = (Listeners) getField(ref, "listeners");
        while(l!=null){
            found.add(l.listener);
            l = l.next;
        }
        assertEquals(expected, found);
    }

    public static void assertHasNoListeners(BetaTransactionalObject ref){
        assertHasListeners(ref);
    }

    public static Object getField(Object o, String fieldname) {
        try {
            Field field = findField(o.getClass(),fieldname);
            field.setAccessible(true);
            return field.get(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field findField(Class clazz, String fieldname){
        try {
            return clazz.getDeclaredField(fieldname);
        } catch (NoSuchFieldException e) {
            if(clazz.equals(Object.class)){
                return null;
            }

            return findField(clazz.getSuperclass(),fieldname);
        }
    }

    public static void assertNotEquals(long l1, long l2) {
        assertFalse(format("both values are %s, but should not be equal", l2), l1 == l2);
    }

    public static void assertNew(BetaTransaction tx) {
        assertEquals(TransactionStatus.New, tx.getStatus());
    }

    public static void assertPrepared(BetaTransaction tx) {
        assertEquals(TransactionStatus.Prepared, tx.getStatus());
    }

    public static void assertAborted(BetaTransaction tx) {
        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    public static void assertCommitted(BetaTransaction tx) {
        assertEquals(TransactionStatus.Committed, tx.getStatus());
    }

    public static void assertActive(BetaTransaction tx) {
        assertEquals(TransactionStatus.Active, tx.getStatus());
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm) {
        return createReadBiasedLongRef(stm, 0);
    }

    public static int randomInt(int max) {
        if (max <= 0) {
            return 0;
        }

        return ThreadLocalRandom.current().nextInt(max);
    }

    public static void sleepRandomMs(int maxMs) {
          Bugshaker.sleepUs((long) randomInt((int)TimeUnit.MILLISECONDS.toMicros(maxMs)));
      }

       
    public static void sleepMs(long ms) {
        long us = TimeUnit.MILLISECONDS.toMicros(ms);
        Bugshaker.sleepUs(us);
    }

    public static LongRef createReadBiasedLongRef(BetaStm stm, long value) {
        LongRef ref = StmUtils.createLongRef(stm, value);

        for (int k = 0; k < ref.getOrec().getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new MonoBetaTransaction(stm);
            tx.openForRead(ref, false, new ObjectPool());
            tx.commit(new ObjectPool());
            assertUnlocked(ref.getOrec());
        }

        assertTrue(ref.getOrec().isReadBiased());

        return ref;
    }
}
