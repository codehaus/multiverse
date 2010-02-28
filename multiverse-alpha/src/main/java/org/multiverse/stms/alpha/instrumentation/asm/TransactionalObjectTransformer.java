package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.instrumentation.metadata.FieldMetadata;
import org.multiverse.stms.alpha.instrumentation.metadata.MetadataRepository;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;

/**
 * An object responsible for enhancing TransactionalObjects. It makes sure that an TransactionalObject implements the {@link
 * org.multiverse.stms.alpha.AlphaTransactionalObject} interface.
 * <p/>
 * It does the following things:
 * <ol>
 * <li>All managed fields are removed (copied to the tranlocal).</li>
 * <li>All instance methods become transactional object.</li>
 * <li>All method content is moved to the tranlocal version of the method</li>
 * </ol>
 * <p/>
 * An instance should not be reused.
 * <p/>
 * The constructor of the donor is not copied. So what out with relying on a constructor in the donor.
 *
 * @author Peter Veentjer
 */
public class TransactionalObjectTransformer implements Opcodes {

    private final ClassNode classNode;
    private final ClassNode mixinClassNode;
    private ClassMetadata classMetadata;

    public TransactionalObjectTransformer(ClassNode originalClass, ClassNode mixinClassNode) {
        this.classNode = originalClass;
        this.classMetadata = MetadataRepository.INSTANCE.getClassMetadata(originalClass.name);
        this.mixinClassNode = mixinClassNode;
    }

    public ClassNode transform() {
        if (classMetadata.isIgnoredClass() || !classMetadata.isRealTransactionalObject()) {
            return null;
        }

        ensureNoProblems();

        removeManagedFields();

        fixUnmanagedFields();

        //if (classMetadata.isFirstGeneration()) {
        mergeMixin();
        // }

        classNode.methods.add(createOpenUnconstructedMethod());

        return classNode;
    }

    private void ensureNoProblems() {
        ////todo: only checks direct super class.
        //if (metadataRepository.isRealAtomicObject(atomicObject.superName)) {
        //    String message = format(
        //            "Subclassing an atomicobject is not allowed. Subclass is %s and the superclass is %s",
        //            atomicObject.name, atomicObject.superName);
        //    throw new IllegalStateException(message);
        //}

        //check for conflicting fields 
        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            if (fieldNode.name.startsWith("___")) {
                String msg = format("Field '%s.%s' begin with illegal pattern '___'",
                        classNode.name,
                        fieldNode.name);
                throw new IllegalStateException(msg);
            }
        }

        //check for conflicting method names
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            if (methodNode.name.startsWith("___")) {
                String msg = format("Method '%s.%s%s' begins with illegal patterns '___'",
                        classNode.name,
                        methodNode.name,
                        methodNode.desc);
                throw new IllegalStateException(msg);
            }
        }
    }

    /**
     * All unmanaged fiels are fixed so that the final access modifier is removed and they are made public (so the
     * tranlocals can access them). The final also needs to be removed because the assignment to the final is done in
     * the tranlocal.
     */
    private void fixUnmanagedFields() {
        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

            if (!fieldMetadata.isManagedField()) {
                fieldNode.access = upgradeToPublic(fieldNode.access);
                if (isFinal(fieldNode.access)) {
                    fieldNode.access -= ACC_FINAL;
                }
            }
        }
    }

    private void removeManagedFields() {
        List<FieldNode> fixedFields = new LinkedList<FieldNode>();

        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

            if (!fieldMetadata.isManagedField()) {
                fixedFields.add(fieldNode);
            }
        }

        classNode.fields = fixedFields;
    }

    private void mergeMixin() {
        mergeStaticInitializers();
        mergeMixinInterfaces();
        mergeMixinFields();
        mergeMixinMethods();
    }

    private void mergeStaticInitializers() {
        Remapper remapper = new SimpleRemapper(mixinClassNode.name, classNode.name);

        MethodNode mixinStaticInit = findStaticInitializer(mixinClassNode);

        if (mixinStaticInit != null) {
            MethodNode txObjectStaticInit = findStaticInitializer(classNode);

            if (txObjectStaticInit == null) {
                MethodNode remappedInit = remap(mixinStaticInit, remapper);
                classNode.methods.add(remappedInit);
            } else {
                MethodNode originalStaticInit = remap(mixinStaticInit, remapper);
                originalStaticInit.name = "___clinit_mixin";
                txObjectStaticInit.name = "___clinit_txobject";

                MethodNode replacementStaticInit = new MethodNode();
                replacementStaticInit.name = "<clinit>";
                replacementStaticInit.desc = txObjectStaticInit.desc;
                replacementStaticInit.access = mixinStaticInit.access;
                replacementStaticInit.tryCatchBlocks = new LinkedList();
                replacementStaticInit.exceptions = new LinkedList();
                replacementStaticInit.localVariables = new LinkedList();
                replacementStaticInit.visitMethodInsn(INVOKESTATIC, classNode.name, originalStaticInit.name, "()V");
                replacementStaticInit.visitMethodInsn(INVOKESTATIC, classNode.name, txObjectStaticInit.name, "()V");
                replacementStaticInit.visitInsn(RETURN);

                classNode.methods.add(replacementStaticInit);
                classNode.methods.add(originalStaticInit);
            }
        }
    }

    private void mergeMixinInterfaces() {
        Set<String> interfaces = new HashSet<String>();

        interfaces.addAll(classNode.interfaces);
        interfaces.addAll(mixinClassNode.interfaces);

        classNode.interfaces = new LinkedList<String>(interfaces);
    }

    private void mergeMixinFields() {
        for (FieldNode mixinField : (List<FieldNode>) mixinClassNode.fields) {
            classNode.fields.add(mixinField);
        }
    }

    private void mergeMixinMethods() {
        Remapper remapper = new SimpleRemapper(mixinClassNode.name, classNode.name);

        for (MethodNode mixinMethodNode : (List<MethodNode>) mixinClassNode.methods) {
            //all constructors and static constructors of the mixinClassNode are dropped
            if (!mixinMethodNode.name.equals("<init>") && !mixinMethodNode.name.equals("<clinit>")) {
                MethodNode remappedMethod = remap(mixinMethodNode, remapper);
                classNode.methods.add(remappedMethod);
            }
        }
    }

    private MethodNode findStaticInitializer(ClassNode classNode) {
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            if (methodNode.name.equals("<clinit>")) {
                return methodNode;
            }
        }

        return null;
    }

    /**
     * If the transactionalObject is not a firstGeneration, the method of the parent
     * TransactionalObject is completely overridden.
     */
    private MethodNode createOpenUnconstructedMethod() {
        String desc = "()" + Type.getDescriptor(AlphaTranlocal.class);

        MethodNode m = new MethodNode(ACC_PUBLIC + ACC_SYNTHETIC, "___openUnconstructed", desc, null, new String[]{});
        m.visitTypeInsn(NEW, classMetadata.getTranlocalName());
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        String constructorDesc = format("(%s)V", internalToDesc(classNode.name));
        m.visitMethodInsn(INVOKESPECIAL, classMetadata.getTranlocalName(), "<init>", constructorDesc);
        m.visitInsn(ARETURN);
        return m;
    }
}
