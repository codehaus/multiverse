package org.multiverse.stms.alpha.instrumentation.metadata;

public final class FieldMetadata {

    private final ClassMetadata classMetadata;
    private String name;
    private String desc;
    private boolean hasFieldGranularity;
    private boolean isManagedField;

    public FieldMetadata(ClassMetadata classMetadata, String name) {
        this.classMetadata = classMetadata;
        this.name = name;
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
