package org.multiverse.instrumentation.metadata;

import org.multiverse.instrumentation.asm.AsmUtils;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the metadata for a method.
 *
 * @author Peter Veentjer.
 */
public final class MethodMetadata {

    private final ClassMetadata classMetadata;

    private TransactionMetadata transactionMetadata;
    private final String name;
    private final String desc;
    private int access;
    private List<String> exceptions = new LinkedList<String>();

    public MethodMetadata(ClassMetadata classMetadata, String name, String desc) {
        this.classMetadata = classMetadata;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Adds an exception (internal name) to the set of exceptions this method can throw. Call is
     * ignored if already added.
     *
     * @param exception
     * @throws NullPointerException if exception is null.
     */
    public void addException(String exception) {
        if (exception == null) {
            throw new NullPointerException();
        }

        if (!exceptions.contains(exception)) {
            exceptions.add(exception);
        }
    }

    /**
     * Only checks if the method explicitly throws this exception. No check is done
     * on the subclass of the exception, needs to be added in the future.
     *
     * @param exception
     * @return
     */
    public boolean checkIfSpecificTransactionIsThrown(Class exception) {
        String type = Type.getInternalName(exception);

        for (String e : exceptions) {
            if (type.equals(e)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    public boolean isAbstract() {
        return AsmUtils.isAbstract(access);
    }

    public boolean isNative() {
        return AsmUtils.isNative(access);
    }

    public boolean isStatic() {
        return AsmUtils.isStatic(access);
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

    public String toFullName() {
        return classMetadata.getName() + "." + name + desc;
    }
}
