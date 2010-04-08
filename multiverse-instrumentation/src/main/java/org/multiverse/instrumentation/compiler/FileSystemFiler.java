package org.multiverse.instrumentation.compiler;

import org.multiverse.instrumentation.asm.AsmUtils;

import java.io.File;

/**
 * @author Peter Veentjer
 */
public class FileSystemFiler implements Filer {

    private final File dumpDirectory;

    public FileSystemFiler(File dumpDirectory) {
        if (dumpDirectory == null) {
            throw new NullPointerException();
        }

        this.dumpDirectory = dumpDirectory;
    }

    @Override
    public void createClassFile(Clazz clazz) {
        File file = new File(dumpDirectory, clazz.getName() + ".class");
        AsmUtils.writeToFile(file, clazz.getBytecode());
    }
}