package org.multiverse.stms.alpha.instrumentation.metadata;

import org.multiverse.stms.alpha.instrumentation.asm.AsmUtils;

public final class MethodMetadata {

    private final ClassMetadata classMetadata;

    private TransactionMetadata transactionMetadata;

    private final String name;

    private final String desc;

    private int access;

    public MethodMetadata(ClassMetadata classMetadata, String name, String desc) {
        this.classMetadata = classMetadata;
        this.name = name;
        this.desc = desc;
    }

    public boolean isAbstract() {
        return AsmUtils.isAbstract(access);
    }

    public boolean isNative() {
        return AsmUtils.isNative(access);
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public boolean isConstructor() {
        return name.equals("<init>");
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public boolean isTransactional() {
        return transactionMetadata != null;
    }

    public ClassMetadata getClassMetadata() {
        return classMetadata;
    }

    public TransactionMetadata getTransactionalMetadata() {
        return transactionMetadata;
    }

    public void setTransactionalMetadata(TransactionMetadata transactionMetadata) {
        this.transactionMetadata = transactionMetadata;
    }
}
