package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.api.ThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static java.lang.String.format;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.isCategory2;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

/**
 * A MethodAdapter that transforms all field access on transactional objects to the correct form. So if a
 * Tranlocal is needed, it will be pushed in between, so:
 * <p/>
 * person.firstname -> person.persontranlocal.firstname
 * <p/>
 * Where the persontranlocal is retrieved from the current transaction.
 */
public class TransactionalObjectRemappingMethodAdapter extends MethodAdapter implements Opcodes {

    private final MetadataRepository metadataRepository;
    private final MethodNode originalMethod;
    private final ClassNode owner;
    private final boolean isTransactionalMethod;

    public TransactionalObjectRemappingMethodAdapter(MethodVisitor mv, ClassNode owner, MethodNode originalMethod) {
        super(mv);
        this.metadataRepository = MetadataRepository.INSTANCE;
        this.owner = owner;
        this.originalMethod = originalMethod;
        this.isTransactionalMethod = metadataRepository.isTransactionalMethod(owner, originalMethod);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String valueDesc) {
        String tranlocalName = metadataRepository.getTranlocalName(owner);

        if (metadataRepository.isManagedInstanceField(owner, name)) {
            switch (opcode) {
                case GETFIELD:
                    txObjectOnTopToTranlocal(owner, false);
                    mv.visitFieldInsn(GETFIELD, tranlocalName, name, valueDesc);
                    break;
                case PUTFIELD:
                    if (isCategory2(valueDesc)) {
                        //value(category2), owner(txobject),..

                        mv.visitInsn(DUP2_X1);
                        //[value(category2), owner(txobject), value(category2),...]

                        mv.visitInsn(POP2);
                        //[owner(txobject), value(category2), ...]
                    } else {
                        //[value(category1), owner(txobject),..
                        mv.visitInsn(SWAP);
                        //[owner(txobject), value(category1),..
                    }

                    txObjectOnTopToTranlocal(owner, true);

                    //Label continueWithPut = new Label();
                    //mv.visitInsn(DUP);
                    //mv.visitFieldInsn(GETFIELD, tranlocalName, "___writeVersion", "J");
                    //mv.visitLdcInsn(new Long(0));
                    //mv.visitInsn(LCMP);
                    //if version equals 0 then continueWithPut

                    //mv.visitJumpInsn(IFEQ, continueWithPut);

                   // mv.visitTypeInsn(NEW, Type.getInternalName(ReadonlyException.class));
                   // mv.visitInsn(DUP);
                   // String msg = format(
                   //         "Can't write on committed field %s.%s. The cause of this error is probably an update " +
                   //                 "in a readonly transaction", owner, name);
                    //
                   // mv.visitLdcInsn(msg);
                   // mv.visitMethodInsn(
                   //         INVOKESPECIAL,
                   //         Type.getInternalName(ReadonlyException.class),
                   //         "<init>",
                   //         "(Ljava/lang/String;)V");
                   // mv.visitInsn(ATHROW);
                   //
                   // mv.visitLabel(continueWithPut);

                    //[value(txobject), owner(tranlocal),..

                    if (isCategory2(valueDesc)) {
                        //[owner(tranlocal), value(category2),..

                        mv.visitInsn(DUP_X2);
                        //[owner(tranlocal), value(category2), owner(tranlocal)

                        mv.visitInsn(POP);
                        //[value(category2), owner(tranlocal),..
                    } else {
                        //[value(category1), owner(txobject),..
                        mv.visitInsn(SWAP);
                        //[owner(txobject), value(category1),..
                    }

                    mv.visitFieldInsn(PUTFIELD, tranlocalName, name, valueDesc);
                    //[..
                    break;
                case GETSTATIC:
                    throw new RuntimeException(format("GETSTATIC on instance field %s.%s not possible", owner, name));
                case PUTSTATIC:
                    throw new RuntimeException(format("PUTSTATIC on instance field %s.%s not possible", owner, name));
                default:
                    throw new RuntimeException();
            }
        } else {
            //fields of unmanaged objects can be used as is, no need for change.
            mv.visitFieldInsn(opcode, owner, name, valueDesc);
        }

        //System.out.println("end "+owner+"."+name+" opcode="+opcode);
    }

    private void txObjectOnTopToTranlocal(String txObjectName, boolean update) {
        if (txObjectName.contains("__")) {
            throw new RuntimeException("No generated classes are allowed: " + txObjectName);
        }

        //if (isTransactionalMethod) {
        //    if (isStatic(originalMethod)) {
        //        super.visitVarInsn(ALOAD, 0);
        //    } else {
        //        super.visitVarInsn(ALOAD, 1);
        //    }
        //} else {
        super.visitMethodInsn(
                INVOKESTATIC,
                getInternalName(ThreadLocalTransaction.class),
                "getRequiredThreadLocalTransaction",
                format("()%s", getDescriptor(Transaction.class)));
        //}

        super.visitInsn(SWAP);

        super.visitTypeInsn(CHECKCAST, txObjectName);

        String method = update ? "openForWrite" : "openForRead";

        super.visitMethodInsn(
                INVOKEINTERFACE,
                getInternalName(AlphaTransaction.class),
                method,
                format("(%s)%s", getDescriptor(AlphaTransactionalObject.class), getDescriptor(AlphaTranlocal.class)));

        String tranlocalName = metadataRepository.getTranlocalName(txObjectName);

        super.visitTypeInsn(CHECKCAST, tranlocalName);
    }
}
