package org.benchy.graph;

import org.benchy.BenchmarkResult;
import org.benchy.TestCaseResult;
import org.benchy.repository.BenchmarkResultRepository;
import org.benchy.repository.FileBasedBenchmarkResultRepository;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Main responsible for reading the {@link org.benchy.repository.BenchmarkResultRepository} and writing output
 * with the GnuPlotGraphWriter.
 *
 * @author Peter Veentjer.
 */
public class GraphMain {

    private final BenchmarkResultRepository resultRepository;
    private final GraphModel model = new GraphModel();

    public GraphMain(File benchmarkDir) {
        resultRepository = new FileBasedBenchmarkResultRepository(benchmarkDir);
    }

    public void doIt(File outputFile, List<String> names, String x, String y) {
        Date now = new Date();

        for (String name : names) {
            addToModel(now, name);
        }

        GraphWriter writer = new GnuPlotGraphWriter(outputFile, x, y);
        writer.write(model);
    }

    public void addToModel(Date date, String benchmarkName) {
        BenchmarkResult benchmarkResult = resultRepository.load(date, benchmarkName);

        for (TestCaseResult caseResult : benchmarkResult.getTestCaseResults()) {
            model.add(benchmarkName, caseResult);
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new RuntimeException();
        }

        System.out.println("Diagram creator");

        File storageDirectory = new File(args[0]);

        File outputFile = new File(args[1]);
        List<String> names = getBenchmarks(args[2]);
        String x = args[3];
        String y = args[4];

        GraphMain graphMain = new GraphMain(storageDirectory);
        graphMain.doIt(outputFile, names, x, y);

        System.out.println("Exiting Diagram Creator");
    }

    private static List<String> getBenchmarks(String s) {
        List<String> benchmarks = new LinkedList<String>();
        StringTokenizer tokenizer = new StringTokenizer(s, ";");
        while (tokenizer.hasMoreTokens()) {
            benchmarks.add(tokenizer.nextToken());
        }
        return benchmarks;
    }
}
