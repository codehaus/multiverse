package org.multiverse.javaagent;

import org.multiverse.instrumentation.Clazz;
import org.multiverse.instrumentation.Instrumentor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
public final class MultiverseClassFileTransformer implements ClassFileTransformer {

    private final static Logger logger = Logger.getLogger(MultiverseClassFileTransformer.class.getName());

    private final Instrumentor compiler;

    public MultiverseClassFileTransformer(Instrumentor compiler) {
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
            System.out.println("MultiverseClassFileTransformer found a problem");
            handleThrowable(className, ex);
            throw ex;
        }
    }

    private static void handleThrowable(String className, Throwable cause) {
        String msg = format("Failed while instrumenting class '%s'. " +
                "It is not possible to abort the instrumentation process, so the JVM is going to continue, " +
                "but since this class is not instrumented, it is not transactional so all bets are off.", className);

        logger.log(Level.SEVERE, msg, cause);

        try {
            JavaAgentProblemMonitor.INSTANCE.signalProblem(className);
        } catch (RuntimeException expected) {
            expected.printStackTrace();
        }
    }
}
