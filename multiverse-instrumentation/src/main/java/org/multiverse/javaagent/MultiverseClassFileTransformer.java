package org.multiverse.javaagent;

import org.multiverse.instrumentation.compiler.Clazz;
import org.multiverse.instrumentation.compiler.ClazzCompiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public class MultiverseClassFileTransformer implements ClassFileTransformer {

    private final static Logger logger = Logger.getLogger(MultiverseClassFileTransformer.class.getName());

    private final ClazzCompiler compiler;

    public MultiverseClassFileTransformer(ClazzCompiler compiler) {
        if (compiler == null) {
            throw new NullPointerException();
        }
        this.compiler = compiler;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("Instrumenting class %s", className));
            }

            Clazz originalClazz = new Clazz(className);
            originalClazz.setBytecode(bytecode);
            originalClazz.setClassLoader(loader);
            Clazz resultClazz = compiler.process(originalClazz);
            return originalClazz == resultClazz ? null : resultClazz.getBytecode();
        } catch (RuntimeException ex) {
            handleThrowable(className, ex);
            throw ex;
        } catch (Error ex) {
            handleThrowable(className, ex);
            throw ex;
        }
    }

    private static void handleThrowable(String className, Throwable ex) {
        String msg = format("Failed while instrumenting class '%s'. " +
                "It is not possible to abort the instrumentation process, so the JVM is going to continue, " +
                "but since this class is partially instrumented, all bets are off.", className);

        logger.log(Level.SEVERE, msg, ex);
        JavaAgentProblemMonitor.INSTANCE.signalProblem();
    }
}
