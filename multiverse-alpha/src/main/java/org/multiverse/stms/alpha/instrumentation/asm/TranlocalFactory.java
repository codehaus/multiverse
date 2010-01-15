package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.internalToDesc;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.upgradeToProtected;
import static org.objectweb.asm.Type.*;

/**
 * A factory responsible for creating the {@link AlphaTranlocal} class based on an {@link org.multiverse.stms.alpha.AlphaTransactionalObject}.
 * <p/>
 * TranlocalClassNodeFactory should not be reused.
 *
 * @author Peter Veentjer
 */
public final class TranlocalFactory implements Opcodes {

    private final ClassNode txObject;
    private final String tranlocalSnapshotName;
    private final String tranlocalName;
    private final MetadataRepository metadataRepo;
    private final boolean isFirstGeneration;
    private final String tranlocalSuperName;

    public TranlocalFactory(ClassNode txObject) {
        this.txObject = txObject;
        this.metadataRepo = MetadataRepository.INSTANCE;
        this.tranlocalName = metadataRepo.getTranlocalName(txObject);
        this.tranlocalSnapshotName = metadataRepo.getTranlocalSnapshotName(txObject);
        this.tranlocalSuperName = metadataRepo.getTranlocalName(txObject.superName);
        this.isFirstGeneration = !metadataRepo.isRealTransactionalObject(txObject.superName);
    }

    public ClassNode create() {
        ClassNode result = new ClassNode();
        result.version = txObject.version;
        result.name = tranlocalName;
        result.access = ACC_PUBLIC + ACC_SYNTHETIC;
        result.sourceFile = txObject.sourceFile;
        result.sourceDebug = txObject.sourceDebug;

        result.fields.addAll(remapInstanceFields());

        if (isFirstGeneration) {
            result.superName = getInternalName(AlphaTranlocal.class);
            result.fields.add(createOriginField());
            result.fields.add(createTxObjectField());
            result.methods.add(createGetOriginMethod());
        } else {
            result.superName = metadataRepo.getTranlocalName(txObject.superName);
        }

        result.methods.add(createOpenForWriteConstructor());
        result.methods.add(createPrepareForCommitMethod());
        result.methods.add(createIsDirtyMethod());
        result.methods.add(createGetTransactionalObjectMethod());
        result.methods.add(createTakeSnapshotMethod());
        result.methods.add(createFreshConstructor());
        result.methods.add(createOpenForWriteMethod());

        return result;
    }

    private Object createOpenForWriteMethod() {
        String desc = "()" + Type.getDescriptor(AlphaTranlocal.class);

        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "openForWrite",
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
                format("(%s)V", internalToDesc(tranlocalName)));
        m.visitInsn(ARETURN);
        return m;
    }

    private List<FieldNode> remapInstanceFields() {
        List<FieldNode> result = new LinkedList<FieldNode>();
        for (FieldNode originalField : metadataRepo.getManagedInstanceFields(txObject)) {

            FieldNode remappedField = new FieldNode(
                    upgradeToProtected(originalField.access),
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
                internalToDesc(tranlocalName), null, null);
    }

    private FieldNode createTxObjectField() {
        return new FieldNode(
                ACC_PUBLIC + ACC_SYNTHETIC + ACC_FINAL,
                "___txObject",
                internalToDesc(txObject.name), null, null);
    }

    private MethodNode createFreshConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalToDesc(txObject.name)),
                null,
                new String[]{});

        if (isFirstGeneration) {
            //we need to call the no arg constructor of the AlphaTranlocal
            m.visitVarInsn(ALOAD, 0);
            m.visitMethodInsn(
                    INVOKESPECIAL, getInternalName(AlphaTranlocal.class), "<init>", "()V");

            //put the atomicobject
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(
                    PUTFIELD, tranlocalName, "___txObject", internalToDesc(txObject.name));
        } else {
            //we need to call the constructor of the superTranlocal with the TransactionalObject
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);

            String superDesc = format("(%s)V", internalToDesc(txObject.superName));
            m.visitMethodInsn(
                    INVOKESPECIAL, tranlocalSuperName, "<init>", superDesc);
        }


        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();

        return m;
    }

    /**
     * Just override the existing one if one exists.
     */
    private MethodNode createOpenForWriteConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalToDesc(tranlocalName)),
                null,
                new String[]{});

        if (isFirstGeneration) {
            m.visitVarInsn(ALOAD, 0);
            m.visitMethodInsn(
                    INVOKESPECIAL, getInternalName(AlphaTranlocal.class), "<init>", "()V");

            //placement of the atomicObject
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(
                    GETFIELD, tranlocalName, "___txObject", internalToDesc(txObject.name));
            m.visitFieldInsn(
                    PUTFIELD, tranlocalName, "___txObject", internalToDesc(txObject.name));

            //placement of the original
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(
                    PUTFIELD, tranlocalName, "___origin", internalToDesc(tranlocalName));
        } else {
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);

            m.visitMethodInsn(
                    INVOKESPECIAL, tranlocalSuperName, "<init>", format("(%s)V", internalToDesc(tranlocalSuperName)));
        }

        //placement of the managed fields.
        for (FieldNode managedField : metadataRepo.getManagedInstanceFields(txObject)) {
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(
                    GETFIELD, tranlocalName, managedField.name, managedField.desc);
            m.visitFieldInsn(
                    PUTFIELD, tranlocalName, managedField.name, managedField.desc);
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    /**
     * Just override the original super.prepareForCommit if one exists.
     */
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
        m.visitFieldInsn(PUTFIELD, tranlocalName, "___origin", internalToDesc(tranlocalName));
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    /**
     * Just override the original super.prepareForCommit if one exists.
     */
    private MethodNode createGetTransactionalObjectMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getTransactionalObject",
                format("()%s", getDescriptor(AlphaTransactionalObject.class)),
                null,
                new String[]{});

        //check on committed
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalName, "___txObject", internalToDesc(txObject.name));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    /**
     * First call the super if one exists, and then continue with
     */
    private MethodNode createIsDirtyMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "isDirty",
                "()Z",
                null,
                new String[]{});

        Label next = new Label();
        if (isFirstGeneration) {
            //check if it is readonly
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(GETFIELD, tranlocalName, "___writeVersion", "J");
            m.visitLdcInsn(new Long(0));
            m.visitInsn(LCMP);
            m.visitJumpInsn(IFEQ, next);
            m.visitInsn(ICONST_0);
            m.visitInsn(IRETURN);

            //check if it is fresh (so has no origin)
            m.visitLabel(next);
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(
                    GETFIELD, tranlocalName, "___origin", internalToDesc(tranlocalName));
            next = new Label();
            m.visitJumpInsn(IFNONNULL, next);
            m.visitInsn(ICONST_1);
            m.visitInsn(IRETURN);
        } else {
            //call the super.getDirtynessStatus
            m.visitVarInsn(ALOAD, 0);
            m.visitMethodInsn(
                    INVOKEVIRTUAL,
                    metadataRepo.getTranlocalName(txObject.superName),
                    "isDirty",
                    "()Z");
            //duplicate the returned value for later use
            //do a comparison, and jump to next if the parent call returned a non clean
            m.visitJumpInsn(IF_ACMPNE, next);
            //so the parent returned a value different than clean,

            m.visitInsn(ICONST_1);
            m.visitInsn(IRETURN);
        }

        m.visitLabel(next);

        List<FieldNode> managedFields = metadataRepo.getManagedInstanceFields(txObject);
        if (!managedFields.isEmpty()) {
            if (isFirstGeneration) {
                m.visitVarInsn(ALOAD, 0);

                m.visitFieldInsn(
                        GETFIELD, tranlocalName, "___origin", internalToDesc(tranlocalName));
            } else {
                m.visitVarInsn(ALOAD, 0);
                //todo: the rootAtomicobject should be used, not the super
                m.visitFieldInsn(
                        GETFIELD, tranlocalSuperName, "___origin", internalToDesc(tranlocalSuperName));
                m.visitTypeInsn(CHECKCAST, tranlocalName);
            }

            //check on managed fields.

            for (FieldNode managedField : managedFields) {
                m.visitInsn(DUP);

                m.visitFieldInsn(
                        GETFIELD, tranlocalName, managedField.name, managedField.desc);
                m.visitVarInsn(ALOAD, 0);
                m.visitFieldInsn(
                        GETFIELD, tranlocalName, managedField.name, managedField.desc);

                next = new Label();
                switch (getType(managedField.desc).getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        m.visitJumpInsn(IF_ICMPEQ, next);
                        break;
                    case Type.FLOAT:
                        m.visitInsn(FCMPL);
                        m.visitJumpInsn(IFEQ, next);
                        break;
                    case Type.LONG:
                        m.visitInsn(LCMP);
                        m.visitJumpInsn(IFEQ, next);
                        break;
                    case Type.DOUBLE:
                        m.visitInsn(DCMPL);
                        m.visitJumpInsn(IFEQ, next);
                        break;
                    case Type.OBJECT:
                        //fall through
                    case Type.ARRAY:
                        m.visitJumpInsn(IF_ACMPEQ, next);
                        break;
                    default:
                        throw new RuntimeException("Unhandled type: " + managedField.desc);
                }

                m.visitInsn(ICONST_1);
                m.visitInsn(IRETURN);
                m.visitLabel(next);
            }
            m.visitLabel(next);
        }

        //this is the last part, where false is returned
        m.visitInsn(ICONST_0);
        m.visitInsn(IRETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    /**
     * Just override the original super.createTakeSnapshotMethod if one exists.
     */
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
        String constructorDesc = format("(%s)V", internalToDesc(tranlocalName));
        m.visitMethodInsn(INVOKESPECIAL, tranlocalSnapshotName, "<init>", constructorDesc);
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }

    /**
     * Just override the original super.createTakeSnapshotMethod if one exists.
     */
    private MethodNode createGetOriginMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getOrigin",
                format("()%s", getDescriptor(AlphaTranlocal.class)),
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(
                GETFIELD, tranlocalName, "___origin", internalToDesc(tranlocalName));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);//value's don't matter, will be reculculated, but call is needed
        m.visitEnd();
        return m;
    }
}
