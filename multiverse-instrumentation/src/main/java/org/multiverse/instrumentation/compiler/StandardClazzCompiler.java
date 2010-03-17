package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.Filer;
import org.multiverse.instrumentation.Resolver;
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
    private boolean dumpClassFiles;
    private Resolver resolver;
    private Filer filer;
    private boolean dumpBytecode;
    private File dumpDir;

    protected final void add(CompilePhase phase) {
        if (compileSteps == null) {
            throw new NullPointerException();
        }

        compileSteps.add(phase);
    }

    @Override
    public void setDumpBytecode(boolean dumpBytecode) {
        this.dumpBytecode = dumpBytecode;
    }

    @Override
    public void setDumpDirectory(File dumpDir) {
        this.dumpDir = dumpDir;
    }

    @Override
    public void prepare() {
        //todo
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
    public Clazz process(Clazz clazz) {
        if (clazz.getClassLoader() == null || isIgnoredPackage(clazz.getName())) {
            return clazz;
        }

        Environment env = new EnvironmentImpl();
        for (CompilePhase step : compileSteps) {
            clazz = step.compile(env, clazz);
        }

        return clazz;
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

    class EnvironmentImpl implements Environment {
        @Override
        public boolean dumpClassFiles() {
            return dumpClassFiles;
        }

        @Override
        public Filer getFiler() {
            return filer;
        }

        @Override
        public boolean isVerbose() {
            return true;
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
}
