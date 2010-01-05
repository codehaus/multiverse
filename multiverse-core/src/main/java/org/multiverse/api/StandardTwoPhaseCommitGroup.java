package org.multiverse.api;

import java.util.LinkedList;
import java.util.List;

public class StandardTwoPhaseCommitGroup implements TwoPhaseCommitGroup{

    private List<Transaction> txList = new LinkedList<Transaction>();

    @Override
    public List<Transaction> getTransaction() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TransactionStatus getStatus() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
