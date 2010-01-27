package org.multiverse.utils.durability;

import org.multiverse.api.Transaction;

import java.io.File;

public class FilebasedTransactionStorage implements TransactionStorage {

    private final File directory;

    public FilebasedTransactionStorage(File directory) {
        if (directory == null) {
            throw new NullPointerException();
        }

        this.directory = directory;
        initDirectory();
    }

    private void initDirectory() {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException();
            }
        } else {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException();
            }
        }

    }

    @Override
    public void writeBehind(Transaction tx) {
        writeThrough(tx);
    }

    @Override
    public void writeThrough(Transaction tx) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
