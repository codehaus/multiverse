package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.instrumentation.metadata.MetadataRepository;
import org.multiverse.utils.instrumentation.InstrumentationProblemMonitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * A convenience ClassFileTransformer implementation. It does the following things: <ol> <li>ignored uninteresting
 * packages like 'java/'</li> <li>signals the InstrumentationProblemMonitor when a problem is encountered</li>
 * <li>prints a stacktrace when a problem is encountered. If you don't catch it here, it will be eaten up</li> </ol>
 *
 * @author Peter Veentjer
 */
public abstract class AbstractClassFileTransformer implements ClassFileTransformer {

    private final static Logger logger = Logger.getLogger(AbstractClassFileTransformer.class.getName());

    public final MetadataRepository metadataRepository = MetadataRepository.INSTANCE;

    private final String transformerName;

    /**
     * Constructs a AbstractClassFileTransformer.
     *
     * @param transformerName a descriptor logging purposes.
     */
    public AbstractClassFileTransformer(String transformerName) {
        this.transformerName = transformerName;
    }

    public abstract byte[] doTransform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException;

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            if (isIgnoredPackage(className)) {
                if (logger.isLoggable(Level.FINE)) {
                    String msg = format("Transformer '%s' is ignoring class '%s' because it is an ignored package",
                            transformerName, className);
                    logger.finer(msg);
                }
                return null;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("Transformer %s is transforming class %s", transformerName, className));
            }

            return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        } catch (RuntimeException ex) {
            handleThrowable(className, ex);
            throw ex;
        } catch (Error ex) {
            handleThrowable(className, ex);
            throw ex;
        }
    }

    private void handleThrowable(String className, Throwable ex) {
        String msg = format("Transformer '%s' failed while instrumenting class '%s'. " +
                "It is not possible to abort the instrumentation process, so the JVM is going to continue, " +
                "but since this class is partially instrumented, all bets are off.", transformerName, className);

        logger.log(Level.SEVERE, msg, ex);
        InstrumentationProblemMonitor.INSTANCE.signalProblem();
    }

    private static boolean isIgnoredPackage(String className) {
        return className.startsWith("java/") ||
                className.startsWith("javax/") ||
                className.startsWith("org/mockito") ||
                className.startsWith("com/jprofiler/") ||
                className.startsWith("org/junit") ||
                className.startsWith("sun/") ||
                className.startsWith("com/sun") ||
                className.startsWith("org/apache/") ||
                className.startsWith("org/hamcrest/") ||
                className.startsWith("com/intellij") ||
                className.startsWith("org/eclipse") ||
                className.startsWith("junit/");
    }
}
