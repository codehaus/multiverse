package org.multiverse.stms.gamma;

import org.multiverse.api.BackoffPolicy;
import org.multiverse.api.ExponentialBackoffPolicy;

public class GammaStmConfiguration {
    public int maxRetries = 1000;
    public int spinCount = 50;
    public BackoffPolicy backoffPolicy = ExponentialBackoffPolicy.MAX_100_MS;
}
