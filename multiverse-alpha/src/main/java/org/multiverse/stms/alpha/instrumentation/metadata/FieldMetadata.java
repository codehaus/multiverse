package org.multiverse.stms.alpha.instrumentation.metadata;

/**
 * Contains the metadata for a class field.
 *
 * @author Peter Veentjer.
 */
public final class FieldMetadata {

    private final ClassMetadata classMetadata;
    private String name;
    private String desc;
    private boolean hasFieldGranularity;
    private boolean isManagedField;
    private int access;

    public FieldMetadata(ClassMetadata classMetadata, String name) {
        if (classMetadata == null || name == null) {
            throw new NullPointerException();
        }

        this.classMetadata = classMetadata;
        this.name = name;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public ClassMetadata getClassMetadata() {
        return classMetadata;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setHasFieldGranularity(boolean hasFieldGranularity) {
        this.hasFieldGranularity = hasFieldGranularity;
    }

    public boolean hasFieldGranularity() {
        return hasFieldGranularity;
    }

    public void setIsManagedField(boolean managedField) {
        this.isManagedField = managedField;
    }

    public boolean isManagedField() {
        return isManagedField;
    }
}
