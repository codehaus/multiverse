package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

/**
 * Shift all arguments on to the right, except the 0 argument (the this).
 *
 * @author Peter Veentjer
 */
public class ShiftArgsToTheRightMethodAdapter extends MethodAdapter {

    public ShiftArgsToTheRightMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (isThisIndex(index)) {
            super.visitLocalVariable(name, desc, signature, start, end, index);
        } else {
            super.visitLocalVariable(name, desc, signature, start, end, index + 1);
        }
    }

    private boolean isThisIndex(int index) {
        return index == 0;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (isThisIndex(var)) {
            mv.visitVarInsn(opcode, var);
        } else {
            mv.visitVarInsn(opcode, var + 1);
        }
    }
}
