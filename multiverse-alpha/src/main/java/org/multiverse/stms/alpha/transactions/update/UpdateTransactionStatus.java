package org.multiverse.stms.alpha.transactions.update;

/**
 * @author Peter Veentjer
 */
public enum UpdateTransactionStatus {

    nowrites {
        @Override
        public UpdateTransactionStatus upgradeToOpenForConstruction() {
            return newonly;
        }

        @Override
        public UpdateTransactionStatus upgradeToOpenForWrite() {
            return updates;
        }
    },

    newonly{
        @Override
        public UpdateTransactionStatus upgradeToOpenForConstruction() {
            return this;
        }

        @Override
        public UpdateTransactionStatus upgradeToOpenForWrite() {
            return updates;
        }
    },

    updates{
        @Override
        public UpdateTransactionStatus upgradeToOpenForConstruction() {
            return this;
        }

        @Override
        public UpdateTransactionStatus upgradeToOpenForWrite() {
            return this;
        }
    };

    public abstract UpdateTransactionStatus upgradeToOpenForConstruction();

    public abstract UpdateTransactionStatus upgradeToOpenForWrite();
}
