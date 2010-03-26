package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.asm.AsmUtils;
import org.multiverse.instrumentation.metadata.MetadataRepository;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class StandardClazzCompiler implements ClazzCompiler {

    private final MetadataRepository metadataRepository = new MetadataRepository();
    private final List<CompilePhase> compileSteps = new LinkedList<CompilePhase>();
    private final String name;
    private final String version;
    private Resolver resolver;
    private Filer filer;
    private boolean dumpBytecode;
    private File dumpDir;

    private Log log = new NullLog();


    public StandardClazzCompiler(String name, String version) {
        if (name == null || version == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.version = version;
    }

    protected final void add(CompilePhase phase) {
        if (compileSteps == null) {
            throw new NullPointerException();
        }

        // if (logger.isLoggable(Level.FINE)) {
        //     logger.fine("Adding CompilerPhase: " + phase.getName());
        // }

        compileSteps.add(phase);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void addExcluded(String ignored) {
        //todo
    }

    @Override
    public void addIncluded(String included) {
        //todo
    }

    @Override
    public void setLog(Log log) {
        if (log == null) {
            this.log = new NullLog();
        } else {
            this.log = log;
        }
    }

    @Override
    public void setDumpBytecode(boolean dumpBytecode) {
        this.dumpBytecode = dumpBytecode;
    }

    @Override
    public void setDumpDirectory(File dumpDirectory) {
        this.dumpDir = dumpDirectory;
    }

    @Override
    public void setFiler(Filer filer) {
        if (filer == null) {
            throw new NullPointerException();
        }
        this.filer = filer;
    }

    @Override
    public void setResolver(Resolver resolver) {
        if (resolver == null) {
            throw new NullPointerException();
        }
        this.resolver = resolver;
    }

    @Override
    public Clazz process(Clazz originalClazz) {
        if (originalClazz.getClassLoader() == null) {
            log.important("Ignoring class '%s' because it is a system class", originalClazz.getName());
            return originalClazz;
        }

        if (isExcluded(originalClazz.getName())) {
            log.important("Ignoring class '%s' because it is excluded", originalClazz.getName());
            return originalClazz;
        }

        log.important("Starting compilation of class '%s'", originalClazz.getName());

        Environment env = new EnvironmentImpl();
        Clazz beforeClazz = originalClazz;
        for (CompilePhase step : compileSteps) {
            Clazz afterClazz = step.compile(env, beforeClazz);
            dump(step, beforeClazz, afterClazz);
            beforeClazz = afterClazz;
        }

        if (originalClazz == beforeClazz) {
            log.important("Finished compilation of class '%s' (class was not modified)", originalClazz.getName());
        } else {
            log.important("Finished compilation of class '%s'", originalClazz.getName());
        }

        return beforeClazz;
    }

    private void dump(CompilePhase step, Clazz beforeClazz, Clazz afterClazz) {
        if (!dumpBytecode) {
            return;
        }

        if (afterClazz == beforeClazz) {
            return;
        }

        File begin = new File(dumpDir, beforeClazz.getName() + "_" + step.getName() + "_before.class");
        AsmUtils.writeToFile(begin, beforeClazz.getBytecode());

        File end = new File(dumpDir, beforeClazz.getName() + "_" + step.getName() + "_after.class");
        AsmUtils.writeToFile(end, afterClazz.getBytecode());
    }

    //todo: ugly hack.

    private static boolean isExcluded(String className) {
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

    class EnvironmentImpl implements Environment {

        @Override
        public boolean dumpBytecode() {
            return dumpBytecode;
        }

        @Override
        public Filer getFiler() {
            if (dumpBytecode) {
                return new DumpingFiler(filer, dumpDir);
            } else {
                return filer;
            }
        }

        @Override
        public Log getLog() {
            return log;
        }

        @Override
        public File getDumpDirectory() {
            return dumpDir;
        }

        @Override
        public MetadataRepository getMetadataRepository() {
            return metadataRepository;
        }
    }

    @Override
    public String toString() {
        return name + "-" + version;
    }
}