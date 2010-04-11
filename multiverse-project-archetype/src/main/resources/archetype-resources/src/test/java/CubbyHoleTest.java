#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.Retry;
import ${package}.CubbyHole;

/**
 * Unit tests for the {@link CubbyHole}.
 * <p>
 * <strong>Note:</strong> The tests require the Multiverse agent in order to run correctly
 * outside Surefire, e.g. in Eclipse. See the configuration of the {@code maven-surefire-plugin}
 * for the correct VM argument to set.
 * 
 * @author Andrew Phillips
 * @see org.multiverse.transactional.DefaultTransactionalReferenceTest
 */
public final class CubbyHoleTest {

    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void constructorLifts() {
        long version = stm.getVersion();

        Transaction tx = txFactory.start();
        setThreadLocalTransaction(tx);

        CubbyHole<Object> cubbyHole = new CubbyHole<Object>();

        tx.commit();
        assertEquals(version, stm.getVersion());
    }

    // ============== rollback =================

    @Test
    public void rollback() {
        rollback(null, null);
        rollback(null, "foo");
        rollback("bar", "foo");
        rollback("bar", null);
    }

    public void rollback(String initialValue, String newContents) {
        CubbyHole<String> cubbyHole = new CubbyHole<String>(initialValue);

        long version = stm.getVersion();

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build()
                .start();
        setThreadLocalTransaction(tx);
        cubbyHole.set(newContents);
        tx.abort();

        assertEquals(version, stm.getVersion());
        assertSame(initialValue, cubbyHole.get());
    }

    // ========== constructors ==================

    @Test
    public void noArgConstruction() {
        long version = stm.getVersion();

        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        assertEquals(version, stm.getVersion());
        assertNull(cubbyHole.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getVersion();

        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        assertEquals(version, stm.getVersion());
        assertNull(cubbyHole.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getVersion();
        String s = "foo";
        CubbyHole<String> cubbyHole = new CubbyHole<String>(s);

        assertEquals(version, stm.getVersion());
        assertEquals(s, cubbyHole.get());
    }

    @Test
    public void testIsNull() {
        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        long version = stm.getVersion();
        assertNull(cubbyHole.get());
        assertEquals(version, stm.getVersion());

        cubbyHole.set("foo");

        version = stm.getVersion();
        assertNotNull(cubbyHole.get());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void testSetFromNullToNull() {
        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        long version = stm.getVersion();
        String result = cubbyHole.set(null);
        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertNull(cubbyHole.get());
    }

    @Test
    public void testSetFromNullToNonNull() {
        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        long version = stm.getVersion();
        String newContents = "foo";
        String result = cubbyHole.set(newContents);
        assertNull(result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newContents, cubbyHole.get());
    }

    @Test
    public void testSetFromNonNullToNull() {
        String oldContents = "foo";
        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();

        String result = cubbyHole.set(null);
        assertSame(oldContents, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(cubbyHole.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldContents = "foo";

        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();

        String newContents = "bar";
        String result = cubbyHole.set(newContents);
        assertSame(oldContents, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newContents, cubbyHole.get());
    }

    @Test
    public void testSetUnchangedReferences() {
        String oldContents = "foo";

        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();

        String result = cubbyHole.set(oldContents);
        assertSame(oldContents, result);
        assertEquals(version, stm.getVersion());
        assertSame(oldContents, cubbyHole.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldContents = new String("foo");

        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();

        String newContents = new String("foo");
        String result = cubbyHole.set(newContents);
        assertSame(oldContents, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newContents, cubbyHole.get());
    }

    @Test
    public void testSetAndUnsetIsNotSeenAsChange() {
        String oldContents = "foo";
        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();
        Transaction tx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build()
                .start();
        setThreadLocalTransaction(tx);
        String newContents = "bar";
        cubbyHole.set(newContents);
        cubbyHole.set(oldContents);
        tx.commit();
        setThreadLocalTransaction(null);

        assertEquals(version, stm.getVersion());
        assertSame(oldContents, cubbyHole.get());
    }

    @Test
    public void getOrAwaitCompletesIfRefNotNull() {
        String oldContents = "foo";

        CubbyHole<String> cubbyHole = new CubbyHole<String>(oldContents);

        long version = stm.getVersion();

        String result = cubbyHole.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldContents, result);
    }

    @Test
    public void getOrAwaitRetriesIfNull() {
        CubbyHole<String> cubbyHole = new CubbyHole<String>();

        long version = stm.getVersion();

        //we start a transaction because we don't want to lift on the retry mechanism
        //of the transaction that else would be started on the getOrAwait method.
        Transaction t = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(t);
        try {
            cubbyHole.getOrAwait();
            fail();
        } catch (Retry retry) {

        }
        assertEquals(version, stm.getVersion());
    }
}
