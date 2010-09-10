package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.functions.Function;

/**
 * @author Peter Veentjer
 */
public class CallableNode {
    public CallableNode next;
    public Function function;

    public CallableNode(Function function, CallableNode next) {
        this.next = next;
        this.function = function;
    }
}
