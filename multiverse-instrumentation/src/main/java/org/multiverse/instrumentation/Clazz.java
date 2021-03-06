package org.multiverse.instrumentation;

import java.util.LinkedList;
import java.util.List;

/**
 * The class to compile. The object should not be changed after being created (so this getter/setter
 * design is questionable but we'll see).
 *
 * @author Peter Veentjer
 */
public class Clazz {

    private final String name;
    private final List<Clazz> createdList = new LinkedList<Clazz>();
    private byte[] bytecode;
    private ClassLoader classLoader;
    private Clazz original;

    public Clazz(String name) {
        this.name = name;
    }

    public Clazz(Clazz original, byte[] bytecode) {
        if (original == null || bytecode == null) {
            throw new NullPointerException();
        }

        this.name = original.name;
        this.original = original;
        this.classLoader = original.classLoader;
        this.bytecode = bytecode;
    }

    public void setOriginal(Clazz original) {
        this.original = original;
    }

    public Clazz getOriginal() {
        return original;
    }

    public List<Clazz> getCreatedList() {
        return createdList;
    }

    public void setBytecode(byte[] bytecode) {
        this.bytecode = bytecode;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return getName().replace("/", ".");
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    public String toString() {
        return name;
    }
}
