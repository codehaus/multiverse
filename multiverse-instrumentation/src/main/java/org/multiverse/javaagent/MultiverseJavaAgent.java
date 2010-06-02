package org.multiverse.javaagent;

import org.multiverse.MultiverseConstants;
import org.multiverse.instrumentation.Instrumentor;
import org.multiverse.instrumentation.SystemOutImportantInstrumenterLogger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.multiverse.utils.SystemOut.println;

/**
 * The MultiverseJavaAgent is responsible for transforming classes when they are loaded
 * using the javaagent technology.
 * <p/>
 * This agent is a general purpose agent that can be used for different STM implementation. And
 * can be configured through System.properties.
 * properties:
 * org.multiverse.javaagent.instrumentor=org.multiverse.stms.alpha.instrumentation.Instrumentor
 * org.multiverse.javaagent.dumpBytecode=true/false
 * org.multiverse.javaagent.verbose=true/false
 * org.multiverse.javaagent.dumpDirectory=directory for dumping classfiles (defaults to the tmp dir)
 * org.multiverse.javaagent.include=pattern of classes to include, seperated by ; or , or : , defaults to
 * everything being included
 * org.multiverse.javaagent.exclude=pattern of classes to exclude, seperated by ; or , or :
 *
 * @author Peter Veentjer
 */
public final class MultiverseJavaAgent implements MultiverseConstants {

    public final static String KEY = "org.multiverse.javaagent.instrumentor";

    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        printMultiverseJavaAgentInfo();

        Instrumentor compiler = loadClazzCompiler();
        inst.addTransformer(new MultiverseClassFileTransformer(compiler));

        println("Multiverse: Multiverse Javaagent started successfully");
    }

    private static Instrumentor loadClazzCompiler() {
        Instrumentor instrumentor = createInstrumentor();

        boolean verbose = getSystemBooleanProperty("verbose", false);
        if (verbose) {
            instrumentor.setLog(new SystemOutImportantInstrumenterLogger());
            println("Multiverse: Verbose output enabled");
        }

        instrumentor.setFiler(new JavaAgentFiler());
        boolean dumpBytecode = getSystemBooleanProperty("dumpBytecode", false);
        instrumentor.setDumpBytecode(dumpBytecode);
        if (dumpBytecode) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            File dumpDirectory = new File(getSystemProperty("dumpDirectory", tmpDir));

            println(format("Multiverse: Bytecode from Javaagent will be dumped to '%s'", dumpDirectory.getAbsolutePath()));

            instrumentor.setDumpDirectory(dumpDirectory);
        }

        String include = include();

        instrumentor.include(include);
        if (instrumentor.getIncluded().equals("")) {
            println("Multiverse: All classes are included since nothing explicitly is configured.");
            println("Multiverse: \tIn most cases you want to set it explicitly using the org.multiverse.javaagent.include System propery.");
        } else {
            println("Multiverse: The following classes are included the instrumentation '%s'" + instrumentor.getIncluded());
        }

        String exclude = exclude();
        instrumentor.exclude(exclude);
        println("Multiverse: The following classes are excluded from instrumentation (exclude overrides includes) " + instrumentor.getExcluded());
        return instrumentor;
    }

    private static String exclude() {
        return normalize(getSystemProperty("exclude", ""));
    }

    private static String include() {
        return normalize(getSystemProperty("include", ""));
    }

    private static String normalize(String s) {
        return s.replace(",", ";").replace(":", ";");
    }

    private static void printMultiverseJavaAgentInfo() {
        println("Multiverse: Starting Multiverse JavaAgent");
        println("Multiverse: Optimizations disabled in Javaaagent (see compiletime instrumentation)");
        if (___BUGSHAKER_ENABLED) {
            println("Multiverse: Bugshaker enabled (can cause performance problems)");
        } else {
            println("Multiverse: Bugshaker disabled (good performance)");
        }

        if (___TRACING_ENABLED) {
            println("Multiverse: Tracing is enabled (useful for debugging/developing)");
        } else {
            println("Multiverse: Tracing is disabled (useful for production environment)");
        }
    }

    private static Instrumentor createInstrumentor() {
        String instrumentorClassName = getSystemProperty(
                "instrumentor",
                "org.multiverse.stms.alpha.instrumentation.AlphaStmInstrumentor");

        println(format("Multiverse: Initializing instrumentor '%s'",
                instrumentorClassName));

        Constructor constructor = getMethod(instrumentorClassName);
        try {
            Instrumentor instrumentor = (Instrumentor) constructor.newInstance();
            println("Multiverse: Initialized '%s-%s'",
                    instrumentor.getName(), instrumentor.getVersion());
            return instrumentor;
        } catch (IllegalAccessException e) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "The constructor is not accessable.",
                    KEY, instrumentorClassName);
            throw new IllegalArgumentException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "The constructor threw an exception.",
                    KEY, instrumentorClassName);
            println(msg);
            throw new IllegalArgumentException(msg, e);
        } catch (InstantiationException e) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "The class could not be instantiated.",
                    KEY, instrumentorClassName);
            println(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static boolean getSystemBooleanProperty(String property, boolean defaultValue) {
        String value = getSystemProperty(property, "" + defaultValue);

        if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else {
            String msg = format("property %s with value '%s' is not a boolean (true/false).", property, value);
            throw new IllegalArgumentException(msg);
        }
    }

    private static String getSystemProperty(String property, String defaultValue) {
        String fullProperty = "org.multiverse.javaagent." + property;
        return getProperty(fullProperty, defaultValue);
    }

    private static Constructor getMethod(String className) {
        Class compilerClazz;
        try {
            compilerClazz = ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "is not an existing class (it can't be found using the Thread.currentThread.getContextClassLoader).",
                    KEY, className);
            println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        if (!Instrumentor.class.isAssignableFrom(compilerClazz)) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "is not an subclass of org.multiverse.compiler.Instrumentor).",
                    KEY, className);
            println(msg);
            throw new IllegalArgumentException(msg);
        }

        Constructor method;
        try {
            method = compilerClazz.getConstructor();
        } catch (NoSuchMethodException e) {
            String msg = format("Failed to initialize Instrumentor through System property '%s' with value '%s'." +
                    "Because a no arg constructor is not found.",
                    KEY, compilerClazz);
            println(msg);
            throw new IllegalArgumentException(msg, e);
        }

        return method;
    }
}
