package org.benchy;

import java.io.*;
import static java.lang.String.format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A {@link org.benchy.BenchmarkResultRepository} that persist on file system.
 * <p/>
 * Implementation is not threadsafe.
 *
 * @author Peter Veentjer.
 */
public class FileBasedBenchmarkResultRepository implements BenchmarkResultRepository {

    private static void ensureExistingDirectory(File dir) {
        if (dir == null) {
            throw new NullPointerException();
        }

        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                String msg = format("Unable to create directory %s", dir);
                throw new RuntimeException(msg);
            }
        }
    }

    private File rootDir;

    /**
     * Creates a FileBasedBenchmarkResultRepository that stores all benchmark results in the temp directory.
     */
    public FileBasedBenchmarkResultRepository() {
        this(System.getProperty("java.io.tmpdir"));
    }

    public FileBasedBenchmarkResultRepository(String rootDir) {
        this(new File(rootDir));
    }

    public FileBasedBenchmarkResultRepository(File rootDir) {
        ensureExistingDirectory(rootDir);

        this.rootDir = rootDir;
    }

    @Override
    public BenchmarkResult loadLast(Date date, String benchmarkName) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public BenchmarkResult loadLast(String benchmark) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public BenchmarkResult load(Date date, String benchmarkName) {
        if (date == null || benchmarkName == null) {
            throw new NullPointerException();
        }

        File benchmarkDirectory = getBenchmarkDirectory(benchmarkName);

        File todaysMeasurementsDir = new File(
                benchmarkDirectory,
                toDateSeperator(date));

        List<TestCaseResult> caseResults = new LinkedList<TestCaseResult>();
        if (todaysMeasurementsDir.isDirectory()) {
            File resultsDir = findLastRun(todaysMeasurementsDir);

            if (resultsDir.isDirectory()) {
                for (File file : resultsDir.listFiles(new ResultFileFilter())) {

                    Properties properties = new Properties();
                    try {
                        properties.load(new FileReader(file));
                        TestCaseResult caseResult = new TestCaseResult(properties);
                        caseResults.add(caseResult);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return new BenchmarkResult(benchmarkName, caseResults);
    }

    public void store(BenchmarkResult benchmarkResult) {
        if (benchmarkResult == null) {
            throw new NullPointerException();
        }

        File targetDir = createTargetDir(benchmarkResult.getBenchmarkName());

        int k = 1;
        for (TestCaseResult result : benchmarkResult.getTestCaseResultList()) {
            File outputFile = new File(targetDir, k + ".txt");
            writeOutputToFile(result, outputFile);
            k++;
        }
    }

    private void writeOutputToFile(TestCaseResult testCaseResult, File target) {
        try {
            Writer output = new BufferedWriter(new FileWriter(target));
            try {
                testCaseResult.getProperties().store(output, "");
            } finally {
                output.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private File createTargetDir(String benchmarkName) {
        File benchmarkDirectory = getBenchmarkDirectory(benchmarkName);

        File todaysMeasurementsDir = new File(
                benchmarkDirectory,
                toDateSeperator(new Date()));

        ensureExistingDirectory(todaysMeasurementsDir);
        return createRunDir(todaysMeasurementsDir);
    }

    /**
     * Creates a directory that points to all files from a run of a single Benchmark.
     */
    private File createRunDir(File todaysBenchmarksDir) {
        int max = 0;
        for (File file : todaysBenchmarksDir.listFiles()) {
            try {
                String name = file.getName();
                int value = Integer.parseInt(name);
                if (value > max) {
                    max = value;
                }
            } catch (NumberFormatException ex) {
                //ignore, go to the next file.
            }
        }

        File runDir = new File(todaysBenchmarksDir, "" + (max + 1));
        ensureExistingDirectory(runDir);
        return runDir;
    }

    private File findLastRun(File todaysBenchmarksDir) {
        int max = -1;
        File result = null;
        for (File file : todaysBenchmarksDir.listFiles()) {
            try {
                String name = file.getName();
                int value = Integer.parseInt(name);
                if (value > max) {
                    max = value;
                    result = file;
                }
            } catch (NumberFormatException ex) {
                //ignore, go to the next file.
            }
        }

        return result;
    }

    private File getBenchmarkDirectory(String benchmarkName) {
        File result = rootDir;
        StringTokenizer tokenizer = new StringTokenizer(benchmarkName);
        while (tokenizer.hasMoreElements()) {
            result = new File(result, tokenizer.nextToken());
        }
        return result;
    }

    private String toDateSeperator(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("y-M-d");
        return format.format(date);
    }

    private static class ResultFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".txt");
        }
    }
}
