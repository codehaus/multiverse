package org.multiverse.utils.profiling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.utils.profiling.ProfileCollator.ProfilePage;

/**
 * Unit tests for the {@link SimpleProfileRepositoryCollator}.
 * 
 * @author Andrew Phillips
 */
public class SimpleProfileRepositoryCollatorTest {
    private SimpleProfileRepository repository;

    @Before
    public void setUp() {
        repository = new SimpleProfileRepository();
    }
    
    @Test
    public void profileNamesContainAtomicObjectAndAOTransactions() {
        repository.incCounter("#kills", "uk/gov/mi6/Agents.meet()");
        repository.incCounter("total damage", "uk/gov/mi6/Agents.performMission()");
        repository.incCounter("#agents", "uk.gov.mi6.Agents");
        
        assertEquals(asSet("atomicObject=uk.gov.mi6.Agents", "aoTransactions=uk/gov/mi6/Agents"), 
                     repository.getCollator().getProfileNames());
    }

    private static <E> Set<E> asSet(E... elems) {
        return new HashSet<E>(Arrays.asList(elems));
    }        

    @Test
    public void profilePageForNonexistentFileIsNull() {
        assertNull(repository.getCollator().getProfilePage("nonexistent"));
    }
    
    @Test
    public void profilePageForAtomicObjectListsSummary() {
        String atomicObjectKey = "uk.gov.mi6.Agents";
        repository.incCounter("#agents", atomicObjectKey, 1);
        repository.incCounter("avg age", atomicObjectKey, 2);
        repository.incCounter("life expectancy", atomicObjectKey, 3);        
        ProfilePage page = repository.getCollator()
                           .getProfilePage("atomicObject=" + atomicObjectKey);
        
        // expect the keys sorted alphabtically
        assertEquals(Arrays.asList("#agents", "avg age", "life expectancy"),
                     page.getStatLabels());
        Map<Object, List<Long>> statistics = page.getStatistics();
        assertEquals(1, statistics.size());
        assertEquals(atomicObjectKey, statistics.keySet().iterator().next());
        assertEquals(Arrays.asList(1L, 2L, 3L), statistics.get(atomicObjectKey));
    }

    @Test
    public void profilePageForAOTransactionsListsMethods() {
        String methodKey1 = "meet()";
        repository.incCounter("#kills", "uk/gov/mi6/Agents." + methodKey1, 1);
        repository.incCounter("expenses", "uk/gov/mi6/Agents." + methodKey1, 2);
        String methodKey2 = "performMission()";
        repository.incCounter("#gadgets", "uk/gov/mi6/Agents." + methodKey2, 3);
        repository.incCounter("total damage", "uk/gov/mi6/Agents." + methodKey2, 4);
        ProfilePage page = repository.getCollator()
                           .getProfilePage("aoTransactions=uk/gov/mi6/Agents");
        
        // expect the keys sorted alphabtically
        assertEquals(Arrays.asList("#gadgets", "#kills", "expenses", "total damage"), 
                     page.getStatLabels());
        Map<Object, List<Long>> statistics = page.getStatistics();
        assertEquals(2, statistics.size());
        assertTrue("Expected key 'meet()'", statistics.containsKey(methodKey1));
        assertEquals(Arrays.asList(null, 1L, 2L, null), statistics.get(methodKey1));
        assertTrue("Expected key 'performMission()'", statistics.containsKey(methodKey2));
        assertEquals(Arrays.asList(3L, null, null, 4L), statistics.get(methodKey2));
    }
    
    @Test
    public void profilePagesContainAtomicObjectAndAOTransactions() {
        repository.incCounter("#kills", "uk/gov/mi6/Agents.meet()");
        repository.incCounter("total damage", "uk/gov/mi6/Agents.performMission()");
        repository.incCounter("#agents", "uk.gov.mi6.Agents");
        
        Collection<String> pageNames = new HashSet<String>();
        for (ProfilePage page : repository.getCollator().getProfilePages()) {
            pageNames.add(page.getName());
        }
        assertEquals(asSet("atomicObject=uk.gov.mi6.Agents", "aoTransactions=uk/gov/mi6/Agents"), 
                     pageNames);
    }    
}
