package org.multiverse.durability;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class StorageUtils {

    public static void saveLines(File file, List<String> lines) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(file, false);
            bufferedWriter = new BufferedWriter(fileWriter);
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.write("\n");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(bufferedWriter);
            closeQuietly(fileWriter);
        }
    }

    public static List<String> loadLines(File file) {
        if (file == null) {
            throw new NullPointerException();
        }

        FileReader in = null;
        BufferedReader reader = null;
        try {
            in = new FileReader(file);
            reader = new BufferedReader(in);
            List<String> result = new LinkedList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals("\n")) {
                    result.add(line);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            closeQuietly(reader);
            closeQuietly(in);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //we don't want instances.

    private StorageUtils() {
    }
}
