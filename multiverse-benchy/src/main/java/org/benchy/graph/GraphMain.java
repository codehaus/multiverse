package org.benchy.graph;

import org.benchy.BenchmarkResult;
import org.benchy.BenchmarkResultRepository;
import org.benchy.FileBasedBenchmarkResultRepository;
import org.benchy.TestCaseResult;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class GraphMain {

    private BenchmarkResultRepository resultRepository;
    private org.benchy.graph.GraphModel model = new org.benchy.graph.GraphModel();

    public GraphMain(File benchmarkDir) {
        resultRepository = new FileBasedBenchmarkResultRepository(benchmarkDir);
    }

    public void doIt(File outputFile, List<String> names, String x, String y) {
        Date now = new Date();

        for (String name : names) {
            addToModel(now, name);
        }

        org.benchy.graph.GraphWriter writer = new org.benchy.graph.GnuPlotGraphWriter(outputFile, x, y);
        writer.write(model);
    }

    public void addToModel(Date date, String benchmarkName) {
        BenchmarkResult benchmarkResult = resultRepository.load(date, benchmarkName);

        for (TestCaseResult caseResult : benchmarkResult.getTestCaseResultList()) {
            model.add(benchmarkName, caseResult);
        }
    }

    public static void main(String[] args) {
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
