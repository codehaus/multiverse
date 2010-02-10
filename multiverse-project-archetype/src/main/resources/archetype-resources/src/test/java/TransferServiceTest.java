#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import java.TransferService;

/**
 * Unit tests for the {@link TransferService}.
 * <p>
 * <strong>Note:</strong> The tests require the Multiverse agent in order to run correctly
 * outside Surefire, e.g. in Eclipse. See the configuration of the {@code maven-surefire-plugin}
 * for the correct VM argument to set.
 */
public class TransferServiceTest {
    private TransferService transferService;

    @Before
    public void setUp() {
        transferService = new TransferService();
    }

    // ============== rollback =================


}