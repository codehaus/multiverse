package org.multiverse.stms.alpha.instrumentation.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public final class ClassMetadata {

    private final String className;
    private final Map<String, FieldMetadata> fields = new HashMap<String, FieldMetadata>();
    private final Map<MethodKey, MethodMetadata> methods = new HashMap<MethodKey, MethodMetadata>();

    private boolean isTransactionalObject;
    private ClassMetadata superClassMetadata;
    private final List<ClassMetadata> interfaces = new LinkedList<ClassMetadata>();
    private boolean ignoredClass;
    private boolean isInterface;

    public ClassMetadata(String className) {
        this.className = className;
    }

    public MethodMetadata createMethodMetadata(String methodName, String desc) {
        if (methodName == null || desc == null) {
            throw new NullPointerException();
        }

        MethodKey methodKey = new MethodKey(methodName, desc);
        if (methods.containsKey(methodKey)) {
            String msg = format("There already is method metadata for %s.%s%s", className, methodName, desc);
            throw new IllegalArgumentException(msg);
        }

        MethodMetadata method = new MethodMetadata(this, methodName, desc);
        methods.put(methodKey, method);
        return method;
    }

    public List<ClassMetadata> getInterfaces() {
        return interfaces;
    }

    public boolean isIgnoredClass() {
        return ignoredClass;
    }

    public void setIgnoredClass(boolean ignoredClass) {
        this.ignoredClass = ignoredClass;
    }

    public ClassMetadata getSuperClassMetadata() {
        return superClassMetadata;
    }

    public void setSuperClassMetadata(ClassMetadata superClassMetadata) {
        this.superClassMetadata = superClassMetadata;
    }

    //todo: problem is with interfaces are not included in search
    public MethodMetadata getMethodMetadata(String name, String desc) {
        MethodMetadata methodMetadata = methods.get(new MethodKey(name, desc));

        if (methodMetadata == null && superClassMetadata != null) {
            methodMetadata = superClassMetadata.getMethodMetadata(name, desc);
        }

        return methodMetadata;
    }

    public FieldMetadata getFieldMetadata(String fieldName) {
        FieldMetadata fieldMetadata = fields.get(fieldName);
        if (fieldMetadata != null) {
            return fieldMetadata;
        }

        if (superClassMetadata != null) {
            fieldMetadata = superClassMetadata.getFieldMetadata(fieldName);
            if (fieldMetadata != null) {
                return fieldMetadata;
            }
        }

        for (ClassMetadata interfaceMetadata : interfaces) {
            fieldMetadata = interfaceMetadata.getFieldMetadata(fieldName);
            if (fieldMetadata != null) {
                return fieldMetadata;
            }
        }

        return null;
    }

    public FieldMetadata createFieldMetadata(String fieldname) {
        if (fieldname == null) {
            throw new NullPointerException();
        }

        if (fields.containsKey(fieldname)) {
            String msg = format("There already is field metadata for %s.%s", className, fieldname);
            throw new IllegalArgumentException(msg);
        }

        FieldMetadata fieldMetadata = new FieldMetadata(this, fieldname);
        fields.put(fieldname, fieldMetadata);
        return fieldMetadata;
    }

    public boolean hasFieldsWithFieldGranularity() {
        for (FieldMetadata field : fields.values()) {
            if (field.hasFieldGranularity()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasManagedFields() {
        for (FieldMetadata field : fields.values()) {
            if (field.isManagedField()) {
                return true;
            }
        }

        return false;
    }

    public String getName() {
        return className;
    }

    public boolean isTransactionalObject() {
        return isTransactionalObject;
    }

    public void setIsTransactionalObject(boolean isTransactionalObject) {
        this.isTransactionalObject = isTransactionalObject;
    }

    public boolean isRealTransactionalObject() {
        if (superClassMetadata != null && superClassMetadata.isRealTransactionalObject()) {
            return true;
        }

        if (isTransactionalObject()) {
            for (FieldMetadata fieldMetadata : fields.values()) {
                if (fieldMetadata.isManagedField()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasTransactionalMethods() {
        for (MethodMetadata methodMetadata : methods.values()) {
            if (methodMetadata.isTransactional()) {
                return true;
            }
        }

        return false;
    }

    public String getTranlocalName() {
        return className + "__Tranlocal";
    }

    public String getTranlocalSnapshotName() {
        return className + "__TranlocalSnapshot";
    }

    public void setIsInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean isInterface() {
        return isInterface;
    }

    static class MethodKey {
        private final String name;
        private final String desc;

        MethodKey(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }

            if (!(thatObject instanceof MethodKey)) {
                return false;
            }

            MethodKey that = (MethodKey) thatObject;
            return this.name.equals(that.name) && this.desc.equals(that.desc);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + 31 * desc.hashCode();
        }
    }
}
