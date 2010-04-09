package org.multiverse.instrumentation.asm;

import org.multiverse.instrumentation.AbstractInstrumentationPhase;
import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Environment;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A {@link org.multiverse.instrumentation.InstrumentationPhase} for inlining jsr instructions.
 * For more information see the {@link org.multiverse.instrumentation.asm.JSRInlineClassAdapter}.
 *
 * @author Peter Veentjer
 */
public class JSRInlineInstrumentationPhase extends AbstractInstrumentationPhase {

    public JSRInlineInstrumentationPhase() {
        super("JSRInlineInstrumentationPhase");
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        JSRInlineClassAdapter inlineAdapter = new JSRInlineClassAdapter(writer);
        ClassReader reader = new ClassReader(originalClazz.getBytecode());
        reader.accept(inlineAdapter, ClassReader.EXPAND_FRAMES);
        byte[] bytecode = writer.toByteArray();
        return new Clazz(originalClazz, bytecode);
    }
}
