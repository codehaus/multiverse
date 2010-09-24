package org.multiverse.stripe;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.references.Ref;

import static org.multiverse.api.StmUtils.newRef;

@Ignore
public class StripeTest {

    @Test
    public void test() {
        int size = 10;
        Ref<String>[] refs = new Ref[size];

        for (int k = 0; k < refs.length; k++) {
            refs[k] = newRef();
        }

        refs[0].ensure();
        refs[1].ensure();
    }
}
