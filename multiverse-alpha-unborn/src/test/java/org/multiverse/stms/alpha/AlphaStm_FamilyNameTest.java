package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class AlphaStm_FamilyNameTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void explicitFamilyName() {
        String familyName = "customfamilyname";
        Transaction tx = stm.getTransactionFactoryBuilder()
                .setFamilyName(familyName)
                .build().start();

        assertEquals(familyName, tx.getConfiguration().getFamilyName());
    }

    @Test
    public void defaultFamilyName() {
        Transaction tx = stm.getTransactionFactoryBuilder().build().start();

        String family1 = tx.getConfiguration().getFamilyName();
        assertTrue(family1.startsWith("TransactionFamily-"));

        long id = getId(family1);
        assertTrue(id > 0);
    }

    @Test
    public void defaultFamilyNamesIncrease() {
        Transaction tx1 = stm.getTransactionFactoryBuilder().build().start();
        Transaction tx2 = stm.getTransactionFactoryBuilder().build().start();

        assertEquals(
                getId(tx1.getConfiguration().getFamilyName()) + 1,
                getId(tx2.getConfiguration().getFamilyName()));
    }

    long getId(String familyName) {
        int indexOf = familyName.indexOf("-");
        return Long.parseLong(familyName.substring(indexOf + 1));
    }
}
