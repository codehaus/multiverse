package org.multiverse.stms.alpha;


import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.TransactionFactoryBuilder;
import org.multiverse.api.programmatic.ProgrammaticRefFactoryBuilder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Sai Venkat
 */
public class GlobalStmInstanceTest {
    private Stm stm;
    private Stm stm1;
    private Stm stm2;

    @Test
    public void GiveMePreConfiguredSTMInstance(){
        stm = getGlobalStmInstance();
        assertThat(stm.getTransactionFactoryBuilder(), is(instanceOf(TransactionFactoryBuilder.class)));
        assertThat(stm.getProgrammaticRefFactoryBuilder(), is(instanceOf(ProgrammaticRefFactoryBuilder.class)));
    }
    @Test
    public void GiveMeSingleInstanceOfSTM() {
        stm1 = getGlobalStmInstance();
        stm2 = getGlobalStmInstance();
        assertThat(stm1, is(sameInstance(stm2)));
    }
}