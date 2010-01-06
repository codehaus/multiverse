package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.DirtinessStatus;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.internalFormToDescriptor;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.upgradeToPublic;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Type.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import static java.lang.String.format;
import java.util.LinkedList;
import java.util.List;

/**
 * A factory responsible for creating the {@link AlphaTranlocal} class based on an {@link AlphaAtomicObject}.
 * <p/>
 * TranlocalClassNodeFactory should not be reused.
 *
 * @author Peter Veentjer
 */
public final class TranlocalFactory implements Opcodes {

    private final ClassNode atomicObject;
    private String tranlocalSnapshotName;
    private String tranlocalName;
    private MetadataRepository metadataService;

    public TranlocalFactory(ClassNode atomicObject) {
        this.atomicObject = atomicObject;
        this.metadataService = MetadataRepository.INSTANCE;
    }

    public ClassNode create() {
        tranlocalName = metadataService.getTranlocalName(atomicObject);
        tranlocalSnapshotName = metadataService.getTranlocalSnapshotName(atomicObject);

        ClassNode result = new ClassNode();
        result.version = atomicObject.version;
        result.name = tranlocalName;
        result.superName = getInternalName(AlphaTranlocal.class);
        result.access = ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC;
        result.sourceFile = atomicObject.sourceFile;
        result.sourceDebug = atomicObject.sourceDebug;

        result.fields.add(createOriginField());
        result.fields.add(createAtomicObjectField());
        result.fields.addAll(remapInstanceFields());
        result.methods.add(createPrivatizeConstructor());
        result.methods.add(createPrepareForCommitMethod());
        result.methods.add(createGetDirtinessStatusMethod());
        result.methods.add(createGetAtomicObjectMethod());
        result.methods.add(createTakeSnapshotMethod());
        result.methods.add(createInitialConstructor());

        return result;
    }

    private List<FieldNode> remapInstanceFields() {
        List<FieldNode> result = new LinkedList<FieldNode>();
        for (FieldNode originalField : metadataService.getManagedInstanceFields(atomicObject)) {

            FieldNode remappedField = new FieldNode(
                    upgradeToPublic(originalField.access),
                    originalField.name,
                    originalField.desc,
                    originalField.signature,
                    originalField.value);
            result.add(remappedField);
        }

        return result;
    }

    private FieldNode createOriginField() {
        return new FieldNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "___origin",
                internalFormToDescriptor(tranlocalName), null, null);
    }

    private FieldNode createAtomicObjectField() {
        return new FieldNode(
                ACC_PUBLIC + ACC_SYNTHETIC + ACC_FINAL,
                "___atomicObject",
                internalFormToDescriptor(atomicObject.name), null, null);
    }

    private MethodNode createInitialConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalFormToDescriptor(atomicObject.name)),
                null,
                new String[]{});

        //reset
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(AlphaTranlocal.class), "<init>", "()V");

        //put the atomicobject
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___atomicObject", internalFormToDescriptor(atomicObject.name));

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();

        return m;
    }

    private MethodNode createPrivatizeConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalFormToDescriptor(tranlocalName)),
                null,
                new String[]{});

        //reset
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(AlphaTranlocal.class), "<init>", "()V");

        //placement of the atomicObject
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(GETFIELD, tranlocalName, "___atomicObject", internalFormToDescriptor(atomicObject.name));
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___atomicObject", internalFormToDescriptor(atomicObject.name));

        //placement of the original
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___origin", internalFormToDescriptor(tranlocalName));

        //placement of the rest of the fields.
        for (FieldNode managedField : metadataService.getManagedInstanceFields(atomicObject)) {
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(GETFIELD, tranlocalName, managedField.name, managedField.desc);
            m.visitFieldInsn(PUTFIELD, tranlocalName, managedField.name, managedField.desc);
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    private MethodNode createPrepareForCommitMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "prepareForCommit",
                "(J)V",
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(LLOAD, 1);
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___writeVersion", "J");

        m.visitVarInsn(ALOAD, 0);
        m.visitInsn(ACONST_NULL);
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___origin", internalFormToDescriptor(tranlocalName));
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    private MethodNode createGetAtomicObjectMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getAtomicObject",
                format("()%s", getDescriptor(AlphaAtomicObject.class)),
                null,
                new String[]{});

        //check on committed
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalName, "___atomicObject", internalFormToDescriptor(atomicObject.name));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    private MethodNode createGetDirtinessStatusMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getDirtinessStatus",
                format("()%s", getDescriptor(DirtinessStatus.class)),
                null,
                new String[]{});

        //check on committed
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalName, "___writeVersion", "J");
        Label failure = new Label();

        m.visitLdcInsn(new Long(0));
        m.visitInsn(LCMP);

        m.visitJumpInsn(IFEQ, failure);
        m.visitFieldInsn(GETSTATIC,
                         getInternalName(DirtinessStatus.class),
                         "committed",
                         getDescriptor(DirtinessStatus.class));
        m.visitInsn(ARETURN);

        //check on original
        m.visitLabel(failure);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalName, "___origin", internalFormToDescriptor(tranlocalName));
        failure = new Label();
        m.visitJumpInsn(IFNONNULL, failure);
        m.visitFieldInsn(GETSTATIC,
                         getInternalName(DirtinessStatus.class),
                         "fresh",
                         getDescriptor(DirtinessStatus.class));
        m.visitInsn(ARETURN);

        //check on arguments
        for (FieldNode managedField : metadataService.getManagedInstanceFields(atomicObject)) {
            m.visitLabel(failure);
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(GETFIELD, tranlocalName, "___origin", internalFormToDescriptor(tranlocalName));
            m.visitFieldInsn(GETFIELD, tranlocalName, managedField.name, managedField.desc);
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(GETFIELD, tranlocalName, managedField.name, managedField.desc);

            failure = new Label();
            switch (getType(managedField.desc).getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    m.visitJumpInsn(IF_ICMPEQ, failure);
                    break;
                case Type.FLOAT:
                    m.visitInsn(FCMPL);
                    m.visitJumpInsn(IFEQ, failure);
                    break;
                case Type.LONG:
                    m.visitInsn(LCMP);
                    m.visitJumpInsn(IFEQ, failure);
                    break;
                case Type.DOUBLE:
                    m.visitInsn(DCMPL);
                    m.visitJumpInsn(IFEQ, failure);
                    break;
                case Type.OBJECT:
                    //fall through
                case Type.ARRAY:
                    m.visitJumpInsn(IF_ACMPEQ, failure);
                    break;
                default:
                    throw new RuntimeException("Unhandled type: " + managedField.desc);
            }

            m.visitFieldInsn(GETSTATIC,
                             getInternalName(DirtinessStatus.class),
                             "fresh",
                             getDescriptor(DirtinessStatus.class));
            m.visitInsn(ARETURN);
        }

        //this is the last part, where the clean value is returned.
        m.visitLabel(failure);
        m.visitFieldInsn(GETSTATIC,
                         getInternalName(DirtinessStatus.class),
                         "clean",
                         getDescriptor(DirtinessStatus.class));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    private MethodNode createTakeSnapshotMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "takeSnapshot",
                format("()%s", getDescriptor(AlphaTranlocalSnapshot.class)),
                null,
                new String[]{});

        m.visitTypeInsn(NEW, tranlocalSnapshotName);
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        String constructorDesc = format("(%s)V", internalFormToDescriptor(tranlocalName));
        m.visitMethodInsn(INVOKESPECIAL, tranlocalSnapshotName, "<init>", constructorDesc);
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }
}
