package org.multiverse.stms.beta;

import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.templates.TransactionTemplate;

public class Queue extends AbstractBetaObject {

    public QueueTranlocal active;

    public Queue(BetaStm stm) {
        new TransactionTemplate(stm) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                BetaTransaction betaTransaction = (BetaTransaction) tx;
                betaTransaction.openForWrite(Queue.this);
                return null;
            }
        }.execute();
    }

    public String pop(BetaStm stm) {
        return new TransactionTemplate<String>(stm) {
            @Override
            public String execute(Transaction tx) throws Exception {
                return pop((BetaTransaction) tx);
            }
        }.execute();
    }

    /**
     * Pop is done on the tail.
     *
     * @param tx
     * @return
     */
    public String pop(BetaTransaction tx) {
        long readVersion = tx.getReadVersion();
        QueueTranlocal tranlocal = (QueueTranlocal) tx.openForRead(this);

        Node tail = tranlocal.tail;
        if (tail == null) {
            throw new IllegalStateException();
        }

        tranlocal = (QueueTranlocal) tx.openForWrite(this);
        if (tranlocal.tail_openForRead(readVersion).next == null) {
            tranlocal = (QueueTranlocal) tx.openForWrite(this);
            tranlocal.head = null;
            tranlocal.head_t = null;

            tranlocal.tail = null;
            tranlocal.tail_t = null;
        } else {
            tranlocal = (QueueTranlocal) tx.openForWrite(this);
            tranlocal.tail = tail.___openForRead(readVersion).prev;
            tranlocal.tail_t = null;

            tranlocal.tail_openForWrite(readVersion);
            tranlocal.tail_t.next = null;
            tranlocal.tail_t.next_t = null;
        }

        //since both branches already have opened the tranlocal for write, no addition openForWriteNeeded.
        //if (tranlocal.___commutingIncFixated) {
        tranlocal.size--;
        //} else {
        //    tranlocal.___commutingInc--;
        //}

        tranlocal.isDirty = true;
        return tail.___openForRead(readVersion).value;
    }

    public void push(final String item, BetaStm stm) {
        new TransactionTemplate(stm) {
            @Override
            public String execute(Transaction tx) throws Exception {
                push(item, (BetaTransaction) tx);
                return null;
            }
        }.execute();
    }

    public void push(String item, BetaTransaction tx) {
        long readVersion = tx.getReadVersion();
        NodeTranlocal node_t = new NodeTranlocal(item);

        QueueTranlocal tranlocal = (QueueTranlocal) tx.openForRead(this);
        if (tranlocal.head == null) {
            tranlocal = (QueueTranlocal) tx.openForWrite(this);
            tranlocal.head = node_t.getTransactionalObject();
            tranlocal.head_t = node_t;

            tranlocal.tail = node_t.getTransactionalObject();
            tranlocal.tail_t = node_t;
        } else {
            node_t.next = tranlocal.head;
            node_t.next_t = tranlocal.head_t;

            tranlocal = (QueueTranlocal) tx.openForWrite(this);
            tranlocal.head_openForWrite(readVersion);
            tranlocal.head_t.prev = node_t.atomicObject;
            tranlocal.head_t.prev_t = node_t;

            tranlocal.head = node_t.getTransactionalObject();
            tranlocal.head_t = node_t;
        }

        tranlocal.isDirty = true;
        //if (tranlocal.___commutingIncFixated) {
        tranlocal.size++;
        //} else {
        //    tranlocal.___commutingInc++;
        //}
    }

    public int size(BetaStm stm) {
        return new TransactionTemplate<Integer>(stm) {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                return size((BetaTransaction) tx);
            }
        }.execute();
    }

    public int size(BetaTransaction tx) {
        QueueTranlocal tranlocal = (QueueTranlocal) tx.openForRead(this);
        if (!tranlocal.___commutingIncFixated) {
            tranlocal.___commutingIncFixated = true;
            tranlocal.size += tranlocal.___commutingInc;
        }
        return tranlocal.size;
    }

    public BetaTranlocal ___openNew() {
        return new QueueTranlocal(this);
    }

    @Override
    public BetaTranlocal ___load() {
        return active;
    }
}


class QueueTranlocal implements BetaTranlocal {
    private long ___version;
    private final Queue ___atomicObject;
    private QueueTranlocal ___origin;

    public boolean isDirty;

    // head
    public Node head;
    public NodeTranlocal head_t;

    //tail
    public Node tail;
    public NodeTranlocal tail_t;

    //size 
    public int size;
    public boolean ___commutingIncFixated;
    public int ___commutingInc;

    public QueueTranlocal(Queue atomicObject) {
        this.___atomicObject = atomicObject;
    }

    public QueueTranlocal(QueueTranlocal origin) {
        this.___atomicObject = origin.___atomicObject;
        this.___origin = origin;
        this.size = origin.size;
        this.tail = origin.tail;
        this.head = origin.head;
    }

    public BetaObject getTransactionalObject() {
        return ___atomicObject;
    }

    @Override
    public QueueTranlocal ___openForWrite() {
        return new QueueTranlocal(this);
    }

    @Override
    public boolean ___isCommitted() {
        return ___version > 0;
    }

    @Override
    public boolean ___isDirty() {
        if (___isCommitted()) {
            return false;
        }

        if (___origin == null) {
            return true;
        }

        return isDirty;
    }

    @Override
    public long ___getWriteVersion() {
        return ___version;
    }

    @Override
    public void ___store(long writeVersion) {
        if (isCommitted()) {
            return;
        }

        ___origin = null;
        ___version = writeVersion;

        if (head_t != null) {
            if (head_t.isDirty()) {
                head_t.___store(writeVersion);
            }
            head_t = null;
        }

        if (tail_t != null) {
            if (tail_t.isDirty()) {
                tail_t.___store(writeVersion);
            }
            tail_t = null;
        }

        //if (!___commutingIncFixated) {
        //    size += ___commutingInc;
        //}

        QueueTranlocal active = ___atomicObject.active;
        if (active != null) {
            //hier kan je dus gaan mergen.
        }

        ___atomicObject.active = this;
    }

    public void ___release() {
        if (___atomicObject.lockOwner == null) {
            return;
        }

        tail_t = null;
        head_t = null;
        ___atomicObject.lockOwner = null;
    }

    public NodeTranlocal tail_openForWrite(long readVersion) {
        if (isCommitted()) {
            throw new IllegalStateException();
        }

        if (tail_t == null) {
            if (tail != null) {
                tail_t = tail.___openForRead(readVersion).___openForWrite();
            }
        } else if (tail_t.___isCommitted()) {
            tail_t = tail_t.___openForWrite();
        }

        return tail_t;
    }

    public void head_openForWrite(long readVersion) {
        if (isCommitted()) {
            throw new IllegalStateException();
        }

        if (head_t == null) {
            if (head != null) {
                head_t = head.___openForRead(readVersion).___openForWrite();
            }
        } else if (head_t.___isCommitted()) {
            head_t = head_t.___openForWrite();
        }
    }

    public boolean isCommitted() {
        return ___version > 0;
    }

    public NodeTranlocal tail_openForRead(long readVersion) {
        if (isCommitted()) {
            throw new IllegalStateException();
        }

        if (tail_t == null) {
            if (tail != null) {
                tail_t = tail.___openForRead(readVersion).___openForWrite();
            }
        }
        return tail_t;
    }

    public void head_openForRead(long readVersion) {
        if (isCommitted()) {
            throw new IllegalStateException();
        }

        if (head_t == null) {
            if (head != null) {
                head_t = head.___openForRead(readVersion).___openForWrite();
            }
        }
    }
}

class Node {
    NodeTranlocal active;

    public NodeTranlocal ___openForRead(long readVersion) {
        NodeTranlocal active = this.active;

        if (active == null) {
            return null;
        }

        if (active.version > readVersion) {
            throw new LoadTooOldVersionException();
        }

        return active;
    }
}

final class NodeTranlocal {

    long version;
    final Node atomicObject;
    NodeTranlocal origin;

    String value;

    Node next;
    NodeTranlocal next_t;

    Node prev;
    NodeTranlocal prev_t;

    NodeTranlocal(String value) {
        this.atomicObject = new Node();
        this.value = value;
    }

    NodeTranlocal(NodeTranlocal origin) {
        this.atomicObject = origin.getTransactionalObject();
        this.origin = origin;
        this.prev = origin.prev;
        this.next = origin.next;
        this.value = origin.value;
    }

    public boolean isDirty() {
        if (___isCommitted()) {
            return false;
        }

        if (origin == null) {
            return true;
        }

        if (origin.value != value) {
            return true;
        }

        if (origin.prev != prev) {
            return true;
        }

        if (origin.next != prev) {
            return true;
        }

        return false;
    }

    public NodeTranlocal next_openForRead(long readVersion) {
        if (___isCommitted()) {
            throw new IllegalStateException();
        }

        if (next_t == null) {
            if (next != null) {
                next_t = next.___openForRead(readVersion);
            }
        }

        return next_t;
    }

    public NodeTranlocal prev_openForRead(long readVersion) {
        if (___isCommitted()) {
            throw new IllegalStateException();
        }

        if (prev_t == null) {
            if (prev != null) {
                prev_t = prev.___openForRead(readVersion);
            }
        }

        return prev_t;
    }

    public NodeTranlocal ___openForWrite() {
        return new NodeTranlocal(this);
    }

    public Node getTransactionalObject() {
        return atomicObject;
    }

    public boolean ___isCommitted() {
        return version > 0;
    }

    public void ___store(long writeVersion) {
        if (___isCommitted()) {
            return;
        }

        //set version first, the prevent cycles.
        origin = null;
        version = writeVersion;

        if (next_t != null) {
            next_t.___store(writeVersion);
            next_t = null;
        }

        if (prev_t != null) {
            prev_t.___store(writeVersion);
            prev_t = null;
        }

        atomicObject.active = this;
    }
}
