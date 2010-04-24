package org.multiverse.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Peter Veentjer
 */
public class IOUtils {

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException drop) {
        }
    }
}
