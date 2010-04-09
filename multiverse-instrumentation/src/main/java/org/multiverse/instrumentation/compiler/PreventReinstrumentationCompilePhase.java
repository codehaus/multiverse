package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.Instrumented;
import org.multiverse.instrumentation.asm.AsmUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedList;

import static java.lang.String.format;
import static org.multiverse.instrumentation.asm.AsmUtils.*;

/**
 * A control flow CompilePhase responsible for preventing that a class already has been
 * instrumented:
 * <ol>
 * <li>If the class already is instrumented, it checks if the compiler name and version
 * match. If not, an error is thrown. Multiverse compilation is not backwards compatible
 * yet.</li>
 * <li><If the class is not instrumented before, it add the {@link Instrumented}
 * with a compilerName and compilerVersion based on the provided compiler. So next
 * time this class is going to be re-instrumented, the multiverse instrumentation
 * is skipped or an error is thrown if the compilerName/compilerVersion don't match.
 * </li>
 * </ol>
 *
 * @author Peter Veentjer
 */
public class PreventReinstrumentationCompilePhase extends AbstractCompilePhase {

    private final static String COMPILER_NAME = "compilerName";
    private final static String COMPILER_VERSION = "compilerVersion";

    private final ClazzCompiler compiler;

    /**
     * Creates a new PreventReinstrumentationCompilePhase.
     *
     * @param compiler the ClazzCompiler that is going to compile the classes.
     * @throws NullPointerException if clazzCompiler is null.
     */
    public PreventReinstrumentationCompilePhase(ClazzCompiler compiler) {
        super("PreventReinstrumentationCompilePhase");
        if (compiler == null) {
            throw new NullPointerException();
        }
        this.compiler = compiler;
    }

    @Override
    protected Clazz doCompile(Environment environment, Clazz originalClazz) {
        ClassNode original = loadAsClassNode(originalClazz.getBytecode());

        AnnotationNode instrumentedAnnotationNode = getVisibleAnnotation(
                original, Instrumented.class);

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
        String foundCompilerName = (String) getAnnotationValue(instrumentedAnnotationNode, COMPILER_NAME);

        if (!compiler.getCompilerName().equals(foundCompilerName)) {
            String msg = format("Failed to instrument already instrumented class '%s'. " +
                    "The current compiler '%s' does not match the previous used compiler '%s' " +
                    "and therefor can't be used in combination with the Stm '%s'. " +
                    "To solve this problem you need to make sure that you using the correct " +
                    "compiler or you need to delete the classes and reinstrument them " +
                    "with this compiler.",
                    original.name, compiler.getCompilerName(), foundCompilerName, compiler.getStmName());
            throw new CompileException(msg);
        }

        String foundCompilerVersion = (String) getAnnotationValue(instrumentedAnnotationNode, COMPILER_VERSION);
        if (!compiler.getCompilerVersion().equals(foundCompilerVersion)) {
            String msg = format("Failed to instrument already instrumented class '%s'. " +
                    "The new compiler version '%s' does not match the previous compiler version. '%s'." +
                    "And b ecause the instrumentation process is not backwards compatible, this" +
                    "class can't be used " +
                    "and therefor can't be used in combination with the current Stm. " +
                    "The Multiverse instrumentation process is not backwards compatible. " +
                    "To solve the problem you need to delete the classes and reinstrument " +
                    "them with this compiler.to solve the problem.",
                    original.name, compiler.getCompilerVersion(), foundCompilerVersion);
            throw new CompileException(msg);
        }
    }

    private AnnotationNode createInstrumentedAnnotationNode() {
        String desc = Type.getType(Instrumented.class).getDescriptor();
        AnnotationNode annotationNode = new AnnotationNode(desc);
        annotationNode.values = new LinkedList();
        annotationNode.values.add(COMPILER_NAME);
        annotationNode.values.add(compiler.getCompilerName());
        annotationNode.values.add(COMPILER_VERSION);
        annotationNode.values.add(compiler.getCompilerVersion());
        return annotationNode;
    }
}
