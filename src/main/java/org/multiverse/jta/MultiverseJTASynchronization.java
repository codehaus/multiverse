package org.multiverse.jta;

import org.multiverse.api.exceptions.TodoException;

import javax.transaction.Synchronization;

public class MultiverseJTASynchronization implements Synchronization {

    @Override
    public void beforeCompletion() {
        throw new TodoException();
    }

    @Override
    public void afterCompletion(int i) {
        throw new TodoException();
    }
}
