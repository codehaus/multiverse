package org.multiverse.instrumentation.asm;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * A {@link org.objectweb.asm.ClassAdapter} that applies {@link org.objectweb.asm.commons.JSRInlinerAdapter}
 * to all methods of a class.
 * <p/>
 * This is needed to let classes containing jrs/ret instructions (before java 5) be transformed to java
 * 5+ classes where jsr/ret is not allowed anymore.
 *
 * @author Peter Veentjer
 */
public final class JSRInlineClassAdapter extends ClassAdapter {

    public JSRInlineClassAdapter(ClassVisitor cv) {
        super(cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
    }
}
