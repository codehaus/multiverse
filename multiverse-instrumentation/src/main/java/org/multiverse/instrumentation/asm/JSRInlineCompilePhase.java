package org.multiverse.instrumentation.asm;

import org.multiverse.instrumentation.compiler.AbstractCompilePhase;
import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.Environment;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A {@link org.multiverse.instrumentation.compiler.CompilePhase} for inlining jsr instructions.
 * For more information see the {@link org.multiverse.instrumentation.asm.JSRInlineClassAdapter}.
 *
 * @author Peter Veentjer
 */
public class JSRInlineCompilePhase extends AbstractCompilePhase {

    public JSRInlineCompilePhase() {
        super("JSRInlineCompilePhase");
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        JSRInlineClassAdapter inlineAdapter = new JSRInlineClassAdapter(writer);
        ClassReader reader = new ClassReader(originalClazz.getBytecode());
        reader.accept(inlineAdapter, ClassReader.EXPAND_FRAMES);
        byte[] bytecode = writer.toByteArray();
        return new Clazz(originalClazz, bytecode);
    }
}
