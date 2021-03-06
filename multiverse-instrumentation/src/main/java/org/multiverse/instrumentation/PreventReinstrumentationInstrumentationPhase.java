package org.multiverse.instrumentation;

import org.multiverse.instrumentation.asm.AsmUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedList;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.*;

/**
 * A control flow InstrumentationPhase responsible for preventing that a class already has been
 * instrumented:
 * <ol>
 * <li>If the class already is instrumented, it checks if the compiler name and version
 * match. If not, an error is thrown. Multiverse compilation is not backwards compatible
 * yet.</li>
 * <li><If the class is not instrumented before, it add the {@link InstrumentationStamp}
 * with a instrumentorName and instrumentorVersion based on the provided compiler. So next
 * time this class is going to be re-instrumented, the multiverse instrumentation
 * is skipped or an error is thrown if the instrumentorName/instrumentorVersion don't match.
 * </li>
 * </ol>
 *
 * @author Peter Veentjer
 */
public class PreventReinstrumentationInstrumentationPhase extends AbstractInstrumentationPhase {

    private final static String INSTRUMENTOR_NAME = "instrumentorName";
    private final static String INSTRUMENTOR_VERSION = "instrumentorVersion";

    private final Instrumentor instrumentor;

    /**
     * Creates a new PreventReinstrumentationInstrumentationPhase.
     *
     * @param instrumentor the Instrumentor that is going to compile the classes.
     * @throws NullPointerException if instrumentor is null.
     */
    public PreventReinstrumentationInstrumentationPhase(Instrumentor instrumentor) {
        super("PreventReinstrumentationInstrumentationPhase");

        if (instrumentor == null) {
            throw new NullPointerException();
        }
        this.instrumentor = instrumentor;
    }

    @Override
    protected Clazz doInstrument(Environment environment, Clazz originalClazz) {
        ClassNode original = loadAsClassNode(originalClazz.getBytecode());

        AnnotationNode instrumentedAnnotationNode = getVisibleAnnotation(
                original, InstrumentationStamp.class);

        if (instrumentedAnnotationNode != null) {
            ensureCorrectClazzCompiler(instrumentedAnnotationNode, original);

            environment.getLog().lessImportant("%s already is instrumented", originalClazz.getName());

            //null indicates that the other compile phases should not be
            //executed anymore.
            return null;
        }

        AnnotationNode annotationNode = createInstrumentedAnnotationNode();
        if (original.visibleAnnotations == null) {
            original.visibleAnnotations = new LinkedList();
        }
        original.visibleAnnotations.add(annotationNode);
        byte[] bytecode = AsmUtils.toBytecode(original);
        return new Clazz(originalClazz, bytecode);
    }

    private void ensureCorrectClazzCompiler(AnnotationNode instrumentedAnnotationNode, ClassNode original) {
        String foundCompilerName = (String) getAnnotationValue(instrumentedAnnotationNode, INSTRUMENTOR_NAME);

        if (!instrumentor.getName().equals(foundCompilerName)) {
            String msg = format("Failed to instrument already instrumented class '%s'. " +
                    "The current instrumentor '%s' does not match the previous used instrumentor '%s' " +
                    "and therefor can't be used in combination with the Stm '%s'. " +
                    "To solve this problem you need to make sure that you using the correct " +
                    "instrumentor or you need to delete the classes and reinstrument them " +
                    "with this instrumentor.",
                    original.name, instrumentor.getName(), foundCompilerName, instrumentor.getStmName());
            throw new CompileException(msg);
        }

        String foundCompilerVersion = (String) getAnnotationValue(instrumentedAnnotationNode, INSTRUMENTOR_VERSION);
        if (!instrumentor.getVersion().equals(foundCompilerVersion)) {
            String msg = format("Failed to instrument already instrumented class '%s'. " +
                    "The new instrumentor version '%s' does not match the previous instrumentor version. '%s'." +
                    "And because the instrumentation process is not backwards compatible, this" +
                    "class can't be used with the current Stm. " +
                    "The Multiverse instrumentation process is not backwards compatible. " +
                    "To solve the problem you need to delete the classes and reinstrument " +
                    "them with this instrumentor %s.%s to solve the problem.",
                    original.name, instrumentor.getVersion(),
                    foundCompilerVersion, instrumentor.getName(),
                    instrumentor.getVersion());
            throw new CompileException(msg);
        }
    }

    private AnnotationNode createInstrumentedAnnotationNode() {
        String desc = Type.getType(InstrumentationStamp.class).getDescriptor();
        AnnotationNode annotationNode = new AnnotationNode(desc);
        annotationNode.values = new LinkedList();
        annotationNode.values.add(INSTRUMENTOR_NAME);
        annotationNode.values.add(instrumentor.getName());
        annotationNode.values.add(INSTRUMENTOR_VERSION);
        annotationNode.values.add(instrumentor.getVersion());
        return annotationNode;
    }
}
