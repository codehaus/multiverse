package org.multiverse.utils.monitoring;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.utils.monitoring.ProfilePublisher.KeyedProfilePage;
import org.multiverse.utils.profiling.ProfileCollator;
import org.multiverse.utils.profiling.ProfileCollator.ProfilePage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ProfilePublisher}
 */
public class ProfilePublisherTest {

    private ProfileCollator collator;
    private ProfilePublisher publisher;

    @Before
    public void setUp() {
        collator = mock(ProfileCollator.class);
        publisher = new ProfilePublisher(collator);
    }

    @Test
    public void getProfileNamesReturnsCollatedNames() {
        when(collator.getProfileNames()).thenReturn(asSet("James", "Bond"));
        String[] profileNames = publisher.getProfileNames();
        Arrays.sort(profileNames);
        assertArrayEquals(new String[]{"Bond", "James"}, profileNames);
    }

    private static <E> Set<E> asSet(E... elems) {
        return new HashSet<E>(Arrays.asList(elems));
    }

    @Test
    public void getStatisticsForUnknownProfileReturnsMessage() {
        String unknownProfileName = "unknown";
        when(collator.getProfilePage(unknownProfileName)).thenReturn(null);
        assertStringContains(publisher.getStatistics(unknownProfileName), unknownProfileName);
    }

    private static void assertStringContains(String total, String... sections) {
        for (String section : sections) {
            assertTrue(String.format("Expected '%s' to contain '%s'", total, section),
                       total.contains(section));
        }
    }

    @Test
    public void KeyedProfilePage_toStringContainsProfilePageData() {
        String profileName = "agents";
        Map<Object, List<Long>> stats = new HashMap<Object, List<Long>>();
        stats.put("007", Arrays.asList(53L, 49L, null));
        KeyedProfilePage<Object> page = new StubKeyedProfilePage(profileName,
                                                                 Arrays.asList("#missions", "#kills"), stats);

        // don't care how the data is layouted, just that it's *there*
        assertStringContains(page.toString(), "#missions", "#kills", "007", "53", "49");
    }

    private static class StubKeyedProfilePage extends KeyedProfilePage<Object> {

        private StubKeyedProfilePage(String name, List<String> statLabels,
                                     Map<Object, List<Long>> stats) {
            super(name, statLabels, stats);
        }

        @Override
        protected String getFormattedKeyLabels() {
            return "";
        }

        @Override
        protected String toFormattedString(Object key) {
            return key.toString();
        }
    }

    @Test
    public void writeToStreamOnlyWritesProfilePageData() throws IOException {
        ProfilePage page = mock(ProfilePage.class);
        when(page.toString()).thenReturn("James Bond");
        when(collator.getProfilePages()).thenReturn(asSet(page));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        publisher.writeStatisticsToStream(stream);
        assertTrue("Expected stream to start with 'James Bond'",
                   stream.toString().startsWith("James Bond"));
    }
}
