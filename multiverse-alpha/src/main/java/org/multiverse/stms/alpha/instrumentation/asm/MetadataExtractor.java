package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.transactional.annotations.TransactionalConstructor;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;

/**
 * An Extractor responsible for collecting information about a ClassNode and storeAndReleaseLock it in the {@link
 * MetadataRepository}. This is one of the first things that should be run, so that the other transformers/factories
 * have their information in place.
 * <p/>
 * An instance should not be reused.
 *
 * @author Peter Veentjer
 */
public final class MetadataExtractor implements Opcodes {

    private boolean isRealTxObject = false;
    private boolean isTxObject = false;
    private boolean hasTxMethods = false;
    private ClassNode classNode;
    private MetadataRepository metadataRepo;

    public MetadataExtractor(ClassNode classNode) {
        this.classNode = classNode;
        this.metadataRepo = MetadataRepository.INSTANCE;
    }

    public void extract() {
        //System.out.println("MetadataExtractor "+classNode.name);

        metadataRepo.signalLoaded(classNode);

        if (isTxObject()) {
            isTxObject = true;

            if (metadataRepo.isRealTransactionalObject(classNode.superName)) {
                isRealTxObject = true;
            }
        }

        extractFieldMetadata();
        extractMethodMetadata();

        metadataRepo.setIsTransactionalObject(classNode, isTxObject);
        metadataRepo.setIsRealTransactionalObject(classNode, isRealTxObject);
        metadataRepo.setHasTransactionalMethods(classNode, hasTxMethods);

        if (isRealTxObject) {
            metadataRepo.setTranlocalName(classNode, classNode.name + "__Tranlocal");
            metadataRepo.setTranlocalSnapshotName(classNode, classNode.name + "__TranlocalSnapshot");
        }
    }

    private boolean isTxObject() {
        if (isInterface(classNode)) {
            return false;
        }

        if (metadataRepo.isTransactionalObject(classNode.superName)) {
            return true;
        }

        if (hasTransactionalObjectAnnotation(classNode)) {
            return true;
        }

        return false;
    }

    private void extractFieldMetadata() {
        for (FieldNode field : (List<FieldNode>) classNode.fields) {
            extractFieldMetadata(field);
        }
    }

    private void extractFieldMetadata(FieldNode field) {
        boolean isManagedField = false;

        if (isManagedField(field)) {
            isRealTxObject = true;
            isManagedField = true;
        }

        metadataRepo.setIsManagedInstanceField(classNode, field, isManagedField);
    }

    /**
     * Checks if the field is a managed field of a transactional object.
     */
    private boolean isManagedField(FieldNode field) {
        return isTxObject &&
                hasDesiredFieldAccess(field.access) &&
                !isExcluded(field);
    }


    private void extractMethodMetadata() {
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            extractMethodMetadata(method);
        }
    }

    private void extractMethodMetadata(MethodNode method) {
        boolean isTxMethod = false;
        TransactionalMethodParams params = null;

        ensureNoTxMethodAccessModifierViolation(method);

        if (isTxObject) {
            if (hasTransactionalMethodAnnotation(method) || hasTransactionalConstructorAnnotation(method)) {
                isTxMethod = true;
                params = createTransactionalMethodParams(method);
            } else if (hasCorrectMethodAccessForTransactionalMethod(method.access) && !isStatic(method)) {
                isTxMethod = true;
                params = createDefaultTransactionalMethodParams(method);
            }
        } else if (hasTransactionalMethodAnnotation(method)) {
            isTxMethod = true;
            params = createTransactionalMethodParams(method);
        } else if (hasTransactionalConstructorAnnotation(method)) {
            isTxMethod = true;
            params = createTransactionalMethodParams(method);
        }

        if (isTxMethod) {
            hasTxMethods = true;
        }

        metadataRepo.setIsTransactionalMethod(classNode, method, isTxMethod);

        if (isTxMethod) {
            metadataRepo.setTransactionalMethodParams(classNode, method, params);
        }
    }

    private void ensureNoTxMethodAccessModifierViolation(MethodNode method) {
        boolean hasTxMethodAnnotation = hasTransactionalMethodAnnotation(method);
        boolean hasInvalidAccessModifier = hasCorrectMethodAccessForTransactionalMethod(method.access);

        if (hasTxMethodAnnotation && !hasInvalidAccessModifier) {
            String msg = "Invalid access modifier for method:" + classNode.name + "." + method.name + method.desc +
                    " (native, abstract and synthetic not allowed) ";
            throw new RuntimeException(msg);
        }
    }

    private TransactionalMethodParams createDefaultTransactionalMethodParams(MethodNode method) {
        if (method.name.equals("<init>")) {
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = false;
            params.automaticReadTracking = true;
            params.detectWriteSkew = true;
            params.retryCount = 0;
            params.familyName = createDefaultFamilyName(method);
            params.interruptible = false;
            params.smartTxLengthSelector = false;
            return params;
        } else {
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = false;
            params.automaticReadTracking = true;
            params.detectWriteSkew = true;
            params.retryCount = 1000;
            params.familyName = createDefaultFamilyName(method);
            params.interruptible = false;
            params.smartTxLengthSelector = true;
            return params;
        }
    }

    private TransactionalMethodParams createTransactionalMethodParams(MethodNode method) {
        if (method.name.equals("<init>")) {
            AnnotationNode txMethodAnnotation = AsmUtils.getVisibleAnnotation(method, TransactionalConstructor.class);
            TransactionalMethodParams params = new TransactionalMethodParams();

            params.familyName = (String) getValue(txMethodAnnotation, "familyName", createDefaultFamilyName(method));
            params.automaticReadTracking = (Boolean) getValue(txMethodAnnotation, "automaticReadTracking", true);
            params.interruptible = (Boolean) getValue(txMethodAnnotation, "interruptible", false);
            params.detectWriteSkew = (Boolean) getValue(txMethodAnnotation, "detectWriteSkew", true);
            params.smartTxLengthSelector = false;
            params.readOnly = (Boolean) getValue(txMethodAnnotation, "readonly", false); 
            return params;
        } else {
            AnnotationNode txMethodAnnotation = AsmUtils.getVisibleAnnotation(method, TransactionalMethod.class);
            TransactionalMethodParams params = new TransactionalMethodParams();
            params.readOnly = (Boolean) getValue(txMethodAnnotation, "readonly", false);
            params.familyName = (String) getValue(txMethodAnnotation, "familyName", createDefaultFamilyName(method));
            params.retryCount = (Integer) getValue(txMethodAnnotation, "retryCount", 1000);
            boolean trackReadsDefault = !params.readOnly;
            params.automaticReadTracking = (Boolean) getValue(txMethodAnnotation, "automaticReadTracking", trackReadsDefault);
            params.interruptible = (Boolean) getValue(txMethodAnnotation, "interruptible", false);
            boolean detectWriteSkewDefault = !params.readOnly;
            params.detectWriteSkew = (Boolean) getValue(txMethodAnnotation, "detectWriteSkew", detectWriteSkewDefault);
            params.smartTxLengthSelector = true;
            return params;
        }
    }

    private String createDefaultFamilyName(MethodNode method) {
        StringBuffer sb = new StringBuffer();
        sb.append(classNode.name.replace("/", "."));
        sb.append('.');
        sb.append(method.name);
        sb.append('(');
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        for (int k = 0; k < argTypes.length; k++) {
            sb.append(argTypes[k].getClassName());
            if (k < argTypes.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }


    private static Object getValue(AnnotationNode node, String name, Object defaultValue) {
        if (node.values == null) {
            return defaultValue;
        }

        for (int k = 0; k < node.values.size(); k += 2) {
            String found = (String) node.values.get(k);
            if (name.equals(found)) {
                return node.values.get(k + 1);
            }
        }

        return defaultValue;
    }

    private static boolean hasCorrectMethodAccessForTransactionalMethod(int access) {
        return !(isSynthetic(access) || isAbstract(access) || isNative(access));
    }

    private static boolean hasDesiredFieldAccess(int access) {
        if (isFinal(access)) {
            return false;
        }

        if (isStatic(access)) {
            return false;
        }

        if (isStatic(access)) {
            return false;
        }

        return true;
    }
}
