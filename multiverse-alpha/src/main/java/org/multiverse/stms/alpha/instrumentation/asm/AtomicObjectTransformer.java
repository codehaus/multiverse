package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.AlphaTranlocal;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

import static java.lang.String.format;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * An object responsible for enhancing AtomicObjects. It makes sure that an AtomicObject implements the {@link
 * org.multiverse.stms.alpha.AlphaAtomicObject} interface.
 * <p/>
 * It does the following things: <ol> <li>All managed fields are removed (copied to the tranlocal).</li> <li>All
 * instance methods become atomic.</li> <li>All method content is moved to the tranlocal version of the method</li>
 * </ol>
 * <p/>
 * An instance should not be reused.
 * <p/>
 * The constructor of the donor is not copied. So what out with relying on a constructor in the donor.
 *
 * @author Peter Veentjer
 */
public class AtomicObjectTransformer implements Opcodes {

    private final ClassNode atomicObject;
    private final ClassNode mixin;
    private final MetadataRepository metadataService;
    private final String tranlocalName;

    public AtomicObjectTransformer(ClassNode atomicObject, ClassNode mixin) {
        this.atomicObject = atomicObject;
        this.mixin = mixin;
        this.metadataService = MetadataRepository.INSTANCE;
        this.tranlocalName = metadataService.getTranlocalName(atomicObject);
    }

    public ClassNode transform() {
        if (!metadataService.hasManagedInstanceFields(atomicObject)) {
            return null;
        }

        ensureNoProblems();

        removeManagedFields();

        fixUnmanagedFields();

        mergeMixin();

        atomicObject.methods.add(createLoadUpdatableMethod());

        return atomicObject;
    }

    private void ensureNoProblems() {
        //todo: only checks direct super class.
        if (metadataService.isRealAtomicObject(atomicObject.superName)) {
            String message = format(
                    "Subclassing an atomicobject is not allowed. Subclass is %s and the superclass is %s",
                    atomicObject.name, atomicObject.superName);
            throw new IllegalStateException(message);
        }

        //check for conflicting fields 
        for (FieldNode fieldNode : (List<FieldNode>) atomicObject.fields) {
            if (fieldNode.name.startsWith("___")) {
                String msg = format("Field '%s.%s' begin with illegal pattern '___'",
                                    atomicObject.name,
                                    fieldNode.name);
                throw new IllegalStateException(msg);
            }
        }

        //check for conflicting method names
        for (MethodNode methodNode : (List<MethodNode>) atomicObject.methods) {
            if (methodNode.name.startsWith("___")) {
                String msg = format("Method '%s.%s%s' begins with illegal patterns '___'",
                                    atomicObject.name,
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
        for (FieldNode field : (List<FieldNode>) atomicObject.fields) {
            if (!metadataService.isManagedInstanceField(atomicObject.name, field.name)) {
                field.access = upgradeToPublic(field.access);
                if (isFinal(field.access)) {
                    field.access -= ACC_FINAL;
                }
            }
        }
    }

    private void removeManagedFields() {
        atomicObject.fields.removeAll(metadataService.getManagedInstanceFields(atomicObject));
    }

    private boolean isConstructor(MethodNode method) {
        return method.name.equals("<init>");
    }

    private void mergeMixin() {
        mergeStaticInitializers();
        mergeMixinInterfaces();
        mergeMixinFields();
        mergeMixinMethods();
    }

    private void mergeStaticInitializers() {
        Remapper remapper = new SimpleRemapper(mixin.name, atomicObject.name);

        MethodNode mixinInit = findStaticInitializer(mixin);
        MethodNode atomicObjectInit = findStaticInitializer(atomicObject);

        if (mixinInit != null) {
            if (atomicObjectInit == null) {
                MethodNode remappedInit = remap(mixinInit, remapper);
                atomicObject.methods.add(remappedInit);
            } else {
                MethodNode remappedInit = remap(mixinInit, remapper);
                remappedInit.name = "___clinit_mixin";
                atomicObjectInit.name = "___clinit_atomicobject";

                MethodNode newInit = new MethodNode();
                newInit.name = "<clinit>";
                newInit.desc = atomicObjectInit.desc;
                newInit.access = mixinInit.access;
                newInit.tryCatchBlocks = new LinkedList();
                newInit.exceptions = new LinkedList();
                newInit.localVariables = new LinkedList();
                newInit.visitMethodInsn(INVOKESTATIC, atomicObject.name, remappedInit.name, "()V");
                newInit.visitMethodInsn(INVOKESTATIC, atomicObject.name, atomicObjectInit.name, "()V");
                newInit.visitInsn(RETURN);

                atomicObject.methods.add(newInit);
                atomicObject.methods.add(remappedInit);
            }
        }
    }


    private void mergeMixinInterfaces() {
        Set<String> interfaces = new HashSet<String>();

        interfaces.addAll(atomicObject.interfaces);
        interfaces.addAll(mixin.interfaces);

        atomicObject.interfaces = new LinkedList<String>(interfaces);
    }

    private void mergeMixinFields() {
        for (FieldNode mixinField : (List<FieldNode>) mixin.fields) {
            atomicObject.fields.add(mixinField);
        }
    }

    private void mergeMixinMethods() {
        Remapper remapper = new SimpleRemapper(mixin.name, atomicObject.name);

        for (MethodNode mixinMethod : (List<MethodNode>) mixin.methods) {
            //all constructors and static constructors of the mixin are dropped
            if (!mixinMethod.name.equals("<init>") && !mixinMethod.name.equals("<clinit>")) {
                MethodNode remappedMethod = remap(mixinMethod, remapper);
                atomicObject.methods.add(remappedMethod);
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

    private MethodNode createLoadUpdatableMethod() {
        String desc = "(J)" + Type.getDescriptor(AlphaTranlocal.class);

        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "___loadUpdatable",
                desc,
                null,
                new String[]{});

        LabelNode startScope = new LabelNode();
        LabelNode endScope = new LabelNode();

        LocalVariableNode tranlocalVar = new LocalVariableNode(
                "___tranlocal",
                internalFormToDescriptor(tranlocalName),
                null,
                startScope,
                endScope,
                3);
        m.localVariables.add(tranlocalVar);

        m.visitLabel(startScope.getLabel());

        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(LLOAD, 1);
        m.visitMethodInsn(INVOKEVIRTUAL, atomicObject.name, "___load", desc);

        //a null check to make sure that a not null value is retrieved.
        m.visitInsn(DUP);
        Label notNull = new Label();
        m.visitJumpInsn(IFNONNULL, notNull);

        //ok, there is no tranlocal, lets create a new instance and return it
        m.visitTypeInsn(NEW, tranlocalName);
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(
                INVOKESPECIAL,
                tranlocalName,
                "<init>",
                format("(%s)V", internalFormToDescriptor(atomicObject.name)));
        m.visitInsn(ARETURN);

        //ok, there is a valid tranlocal, lets make a private copy that can be used for updating
        m.visitLabel(notNull);
        m.visitTypeInsn(CHECKCAST, tranlocalName);
        m.visitVarInsn(ASTORE, 3);
        m.visitTypeInsn(NEW, tranlocalName);
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 3);
        m.visitMethodInsn(
                INVOKESPECIAL,
                tranlocalName,
                "<init>",
                format("(%s)V", internalFormToDescriptor(tranlocalName)));
        m.visitInsn(ARETURN);

        m.visitLabel(endScope.getLabel());
        return m;
    }


}
