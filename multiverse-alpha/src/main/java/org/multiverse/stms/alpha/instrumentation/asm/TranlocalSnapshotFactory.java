package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTranlocalSnapshot;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.internalToDesc;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

/**
 * A Factory responsible for creating the TranlocalSnapshot classes for the TransactionalObject that need one.
 * <p/>
 * TranlocalSnapshotClassNodeFactory should not be reused.
 *
 * @author Peter Veentjer
 */
public class TranlocalSnapshotFactory implements Opcodes {

    private final ClassNode classNode;
    private final String tranlocalName;
    private final String tranlocalSnapshotName;
    private final MetadataRepository metadataService;
    private final ClassMetadata classMetadata;

    public TranlocalSnapshotFactory(ClassLoader classLoader, ClassNode classNode, MetadataRepository metadataRepository) {
        this.metadataService = metadataRepository;
        this.classNode = classNode;
        this.classMetadata = metadataService.getClassMetadata(classLoader, classNode.name);
        this.tranlocalName = classMetadata.getTranlocalName();
        this.tranlocalSnapshotName = classMetadata.getTranlocalSnapshotName();
    }

    public ClassNode create() {
        if (!classMetadata.hasManagedFields()) {
            return null;
        }

        ClassNode result = new ClassNode();
        result.version = classNode.version;
        result.name = tranlocalSnapshotName;
        result.superName = getInternalName(AlphaTranlocalSnapshot.class);
        result.access = ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC;
        result.sourceFile = classNode.sourceFile;
        result.sourceDebug = classNode.sourceDebug;
        result.methods.add(createConstructor());
        result.methods.add(createRestoreMethod());
        result.methods.add(createGetTranlocalMethod());
        result.fields.addAll(createFields());
        return result;
    }

    public List<FieldNode> createFields() {
        List<FieldNode> fields = new LinkedList<FieldNode>();

        FieldNode tranlocalField = new FieldNode(
                ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC,
                "___tranlocal",
                internalToDesc(tranlocalName),
                null,
                null);
        fields.add(tranlocalField);

        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

            if (fieldMetadata.isManagedField()) {
                FieldNode newFieldNode = new FieldNode(
                        ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC,
                        fieldNode.name,
                        fieldNode.desc,
                        fieldNode.signature,
                        null);
                fields.add(newFieldNode);
            }
        }

        return fields;
    }

    public MethodNode createConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalToDesc(tranlocalName)),
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, getInternalName(AlphaTranlocalSnapshot.class), "<init>", "()V");

        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, tranlocalSnapshotName, "___tranlocal", internalToDesc(tranlocalName));

        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

            if (fieldMetadata.isManagedField()) {
                m.visitVarInsn(ALOAD, 0);
                m.visitVarInsn(ALOAD, 1);
                m.visitFieldInsn(GETFIELD, tranlocalName, fieldNode.name, fieldNode.desc);
                m.visitFieldInsn(PUTFIELD, tranlocalSnapshotName, fieldNode.name, fieldNode.desc);
            }
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    public MethodNode createGetTranlocalMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getTranlocal",
                format("()%s", getDescriptor(AlphaTranlocal.class)),
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalSnapshotName, "___tranlocal", internalToDesc(tranlocalName));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    public MethodNode createRestoreMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "restore",
                "()V",
                null,
                new String[]{});

        for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
            FieldMetadata fieldMetadata = classMetadata.getFieldMetadata(fieldNode.name);

            if (fieldMetadata.isManagedField()) {
                //[..
                m.visitVarInsn(ALOAD, 0);
                //[this, ..
                m.visitFieldInsn(GETFIELD, tranlocalSnapshotName, "___tranlocal", internalToDesc(tranlocalName));

                //[tranlocal
                m.visitVarInsn(ALOAD, 0);
                //[this, tranlocal, ...
                m.visitFieldInsn(GETFIELD, tranlocalSnapshotName, fieldNode.name, fieldNode.desc);
                //[value, tranlocal, ..
                m.visitFieldInsn(PUTFIELD, tranlocalName, fieldNode.name, fieldNode.desc);
                //[..
            }
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }
}
