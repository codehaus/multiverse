package org.multiverse.api.predicates;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.predicates.Predicates.newIsNotNullPredicate;
import static org.multiverse.api.predicates.Predicates.newIsNullPredicate;

public class PredicatesTest {

    @Test
    public void testIsNullPredicate() {
        Predicate predicate = newIsNullPredicate();

        assertTrue(predicate.evaluate(null));
        assertFalse(predicate.evaluate(""));
    }

    @Test
    public void testIsNotNullPredicate() {
        Predicate predicate = newIsNotNullPredicate();

        assertFalse(predicate.evaluate(null));
        assertTrue(predicate.evaluate(""));
    }
}
