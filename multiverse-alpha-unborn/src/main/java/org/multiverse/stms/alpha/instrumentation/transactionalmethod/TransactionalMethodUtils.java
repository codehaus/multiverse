package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

/**
 * @author Peter Veentjer
 */
public class TransactionalMethodUtils {

    public static String toTransactedMethodName(String originalMethodName, boolean readonly) {
        if (originalMethodName.equals("<init>")) {
            return originalMethodName;
        }
        return originalMethodName + (readonly ? "___ro" : "___up");
    }

}
