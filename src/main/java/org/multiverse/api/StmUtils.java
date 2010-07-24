package org.multiverse.api;

import org.multiverse.api.exceptions.RetryError;

public class StmUtils {

    public static void retry(){
        throw RetryError.INSTANCE;
    }
}
