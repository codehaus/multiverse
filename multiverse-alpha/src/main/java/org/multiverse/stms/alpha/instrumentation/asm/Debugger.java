package org.multiverse.stms.alpha.instrumentation.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to aid debugging of a method's byte code.
 */
public class Debugger extends TraceMethodVisitor {

    private int instruction;
    private Frame[] frames;

    public static String debug(String owner, MethodNode method) {
        Analyzer analyzer = new Analyzer(new SimpleVerifier());
        try {
            analyzer.analyze(owner, method);
        } catch (Exception e) {
            e.printStackTrace();
            // ignore, we are only interested in the frames
        }
        Frame[] frames = analyzer.getFrames();

        Debugger debugger = new Debugger(frames);
        method.accept(debugger);

        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        debugger.print(writer);
        writer.flush();

        return output.toString();
    }

    public Debugger(Frame[] frames) {
        this.instruction = 0;
        this.frames = frames;
    }

    private void addByteCodeIndexWithoutFrame(int lastSize) {
        String index = "000" + Integer.toString(instruction);
        index = index.substring(index.length() - 4, index.length());

        for (int i = lastSize; i < text.size(); i++) {
            String line = (String) text.get(i);
            text.set(i, index.toUpperCase() + line);
        }

        instruction++;

    }

    private void addByteCodeIndex(int lastSize) {
        Frame frame = frames[instruction];
        if (frame != null) {
            List<String> frameText = new ArrayList<String>();
            for (int i = 0; i < frame.getLocals(); i++) {
                BasicValue local = (BasicValue) frame.getLocal(i);
                if (local != BasicValue.UNINITIALIZED_VALUE) {
                    frameText.add(tab3 + "Local: " + i + ": "
                            + (local.isReference() ? local.getType().getDescriptor() : local) + "\n");
                }
            }
            for (int i = 0; i < frame.getStackSize(); i++) {
                BasicValue stack = (BasicValue) frame.getStack(i);
                frameText.add(
                        tab3 + "Stack: " + i + ": " + (stack.isReference() ? stack.getType().getDescriptor() : stack)
                                + "\n");
            }

            text.addAll(lastSize, frameText);
        }

        addByteCodeIndexWithoutFrame(lastSize);
    }

    private void addNoByteCodeIndex(int lastSize) {
        for (int i = lastSize; i < text.size(); i++) {
            String line = (String) text.get(i);
            text.set(i, "----" + line);
        }
    }

    //
    // instructions
    //

    @Override
    public void visitInsn(int opcode) {
        int lastSize = text.size();
        super.visitInsn(opcode);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        int lastSize = text.size();
        super.visitIntInsn(opcode, operand);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        int lastSize = text.size();
        super.visitVarInsn(opcode, var);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        int lastSize = text.size();
        super.visitTypeInsn(opcode, type);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        int lastSize = text.size();
        super.visitFieldInsn(opcode, owner, name, desc);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        int lastSize = text.size();
        super.visitMethodInsn(opcode, owner, name, desc);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        int lastSize = text.size();
        super.visitJumpInsn(opcode, label);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        int lastSize = text.size();
        super.visitLdcInsn(cst);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        int lastSize = text.size();
        super.visitIincInsn(var, increment);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        int lastSize = text.size();
        super.visitTableSwitchInsn(min, max, dflt, labels);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        int lastSize = text.size();
        super.visitLookupSwitchInsn(dflt, keys, labels);
        addByteCodeIndex(lastSize);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        int lastSize = text.size();
        super.visitMultiANewArrayInsn(desc, dims);
        addByteCodeIndex(lastSize);
    }

    //
    // no instructions, but which asm counts as instructions
    //

    @Override
    public void visitLabel(Label label) {
        int lastSize = text.size();
        super.visitLabel(label);
        addByteCodeIndexWithoutFrame(lastSize);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        int lastSize = text.size();
        super.visitLineNumber(line, start);
        addByteCodeIndexWithoutFrame(lastSize);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        int lastSize = text.size();
        super.visitFrame(type, nLocal, local, nStack, stack);
        addByteCodeIndexWithoutFrame(lastSize);
    }

    //
    //
    //

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        int lastSize = text.size();
        super.visitTryCatchBlock(start, end, handler, type);
        addNoByteCodeIndex(lastSize);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        int lastSize = text.size();
        super.visitLocalVariable(name, desc, signature, start, end, index);
        addNoByteCodeIndex(lastSize);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        int lastSize = text.size();
        super.visitMaxs(maxStack, maxLocals);
        addNoByteCodeIndex(lastSize);
    }
}