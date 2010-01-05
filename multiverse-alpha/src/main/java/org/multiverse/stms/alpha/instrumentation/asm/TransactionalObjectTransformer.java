package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.AlphaTranlocal;
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

    private final ClassNode txObject;
    private final ClassNode mixin;
    private final MetadataRepository metadataRepository;
    private final String tranlocalName;
    private final boolean isFirstGeneration;

    public TransactionalObjectTransformer(ClassNode txObject, ClassNode mixin) {
        this.txObject = txObject;
        this.mixin = mixin;
        this.metadataRepository = MetadataRepository.INSTANCE;

        this.tranlocalName = metadataRepository.getTranlocalName(txObject);
        this.isFirstGeneration = !metadataRepository.isRealTransactionalObject(txObject.superName);
    }

    public ClassNode transform() {
        if (!metadataRepository.hasManagedInstanceFields(txObject)) {
            return null;
        }

        ensureNoProblems();

        removeManagedFields();

        fixUnmanagedFields();

        if (isFirstGeneration) {
            mergeMixin();
        }

        txObject.methods.add(createOpenUnconstructedMethod());

        return txObject;
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
        for (FieldNode fieldNode : (List<FieldNode>) txObject.fields) {
            if (fieldNode.name.startsWith("___")) {
                String msg = format("Field '%s.%s' begin with illegal pattern '___'",
                        txObject.name,
                        fieldNode.name);
                throw new IllegalStateException(msg);
            }
        }

        //check for conflicting method names
        for (MethodNode methodNode : (List<MethodNode>) txObject.methods) {
            if (methodNode.name.startsWith("___")) {
                String msg = format("Method '%s.%s%s' begins with illegal patterns '___'",
                        txObject.name,
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
        for (FieldNode field : (List<FieldNode>) txObject.fields) {
            if (!metadataRepository.isManagedInstanceField(txObject.name, field.name)) {
                field.access = upgradeToPublic(field.access);
                if (isFinal(field.access)) {
                    field.access -= ACC_FINAL;
                }
            }
        }
    }

    private void removeManagedFields() {
        List<FieldNode> managedFields = metadataRepository.getManagedInstanceFields(txObject);
        txObject.fields.removeAll(managedFields);
    }

    private void mergeMixin() {
        mergeStaticInitializers();
        mergeMixinInterfaces();
        mergeMixinFields();
        mergeMixinMethods();
    }

    private void mergeStaticInitializers() {
        Remapper remapper = new SimpleRemapper(mixin.name, txObject.name);

        MethodNode mixinStaticInit = findStaticInitializer(mixin);

        if (mixinStaticInit != null) {
            MethodNode txObjectStaticInit = findStaticInitializer(txObject);

            if (txObjectStaticInit == null) {
                MethodNode remappedInit = remap(mixinStaticInit, remapper);
                txObject.methods.add(remappedInit);
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
                replacementStaticInit.visitMethodInsn(INVOKESTATIC, txObject.name, originalStaticInit.name, "()V");
                replacementStaticInit.visitMethodInsn(INVOKESTATIC, txObject.name, txObjectStaticInit.name, "()V");
                replacementStaticInit.visitInsn(RETURN);

                txObject.methods.add(replacementStaticInit);
                txObject.methods.add(originalStaticInit);
            }
        }
    }

    private void mergeMixinInterfaces() {
        Set<String> interfaces = new HashSet<String>();

        interfaces.addAll(txObject.interfaces);
        interfaces.addAll(mixin.interfaces);

        txObject.interfaces = new LinkedList<String>(interfaces);
    }

    private void mergeMixinFields() {
        for (FieldNode mixinField : (List<FieldNode>) mixin.fields) {
            txObject.fields.add(mixinField);
        }
    }

    private void mergeMixinMethods() {
        Remapper remapper = new SimpleRemapper(mixin.name, txObject.name);

        for (MethodNode mixinMethod : (List<MethodNode>) mixin.methods) {
            //all constructors and static constructors of the mixin are dropped
            if (!mixinMethod.name.equals("<init>") && !mixinMethod.name.equals("<clinit>")) {
                MethodNode remappedMethod = remap(mixinMethod, remapper);
                txObject.methods.add(remappedMethod);
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

        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "___openUnconstructed",
                desc,
                null,
                new String[]{});

        m.visitTypeInsn(NEW, tranlocalName);
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(
                INVOKESPECIAL,
                tranlocalName,
                "<init>",
                format("(%s)V", internalToDesc(txObject.name)));
        m.visitInsn(ARETURN);

        return m;
    }
}
