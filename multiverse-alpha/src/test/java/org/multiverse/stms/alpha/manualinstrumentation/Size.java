package org.multiverse.stms.alpha.manualinstrumentation;

import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.mixins.DefaultTxObjectMixin;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.TodoException;

public class Size extends DefaultTxObjectMixin {

    public Size() {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction atx = (AlphaTransaction) tx;
                SizeTranlocal tranlocal = (SizeTranlocal) atx.openForWrite(Size.this);
                tranlocal.value = 0;
                return null;
            }
        }.execute();
    }

    @Override
    public SizeTranlocal ___openUnconstructed() {
        return new SizeTranlocal(this);
    }

    public void inc() {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                AlphaTransaction atx = (AlphaTransaction) tx;
                SizeTranlocal tranlocal = (SizeTranlocal) atx.openForCommutingOperation(Size.this);
                inc(tranlocal, atx);
                return null;
            }
        }.execute();
    }

    public void inc(SizeTranlocal tranlocal, AlphaTransaction atx) {
        if (tranlocal.isUnfixated()) {
            tranlocal.inc++;
        } else {
            tranlocal.value++;
        }
    }

    public void dec() {
        new TransactionTemplate() {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                AlphaTransaction atx = (AlphaTransaction) tx;
                SizeTranlocal tranlocal = (SizeTranlocal) atx.openForCommutingOperation(Size.this);
                dec(tranlocal, atx);
                return null;
            }
        }.execute();
    }

    public void dec(SizeTranlocal tranlocal, AlphaTransaction atx) {
        if (tranlocal.isUnfixated()) {
            tranlocal.dec++;
        } else {
            tranlocal.value--;
        }
    }

    public int get() {
        return new TransactionTemplate<Integer>() {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                AlphaTransaction atx = (AlphaTransaction) tx;
                SizeTranlocal tranlocal = (SizeTranlocal) atx.openForRead(Size.this);
                return get(tranlocal, atx);
            }
        }.execute();
    }

    public int get(SizeTranlocal tranlocal, AlphaTransaction tx) {
        return tranlocal.value;
    }

    public void set(final int value) {
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                AlphaTransaction atx = (AlphaTransaction) tx;
                SizeTranlocal tranlocal = (SizeTranlocal) atx.openForWrite(Size.this);
                set(value, tranlocal, atx);
                return null;
            }
        }.execute();
    }

    public void set(int value, SizeTranlocal tranlocal, AlphaTransaction tx) {
        tranlocal.value = value;
    }
}


class SizeTranlocal extends AlphaTranlocal {

    private final Size txObject;
    private SizeTranlocal ___origin;
    public int value;
    public int inc;
    public int dec;

    SizeTranlocal(Size txObject) {
        this.txObject = txObject;
    }

    SizeTranlocal(SizeTranlocal origin) {
        this.txObject = origin.txObject;
        this.___origin = origin;
    }

    @Override
    public AlphaTranlocal getOrigin() {
        return ___origin;
    }

    @Override
    public AlphaTranlocal openForWrite() {
        return new SizeTranlocal(this);
    }

    @Override
    public void prepareForCommit(long writeVersion) {
        this.___writeVersion = writeVersion;
        this.___origin = null;
    }

    @Override
    public boolean isDirty() {
        if (isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        if (___origin.value != value) {
            return true;
        }

        return false;
    }

    @Override
    public void fixate(AlphaTransaction tx) {
        //todo: version needs to be set.

        for (int k = 0; k < inc; k++) {
            txObject.inc(this, tx);
        }

        for (int k = 0; k < inc; k++) {
            txObject.dec(this, tx);
        }
    }

    @Override
    public AlphaTransactionalObject getTransactionalObject() {
        return txObject;
    }

    @Override
    public AlphaTranlocalSnapshot takeSnapshot() {
        throw new TodoException();
    }
}