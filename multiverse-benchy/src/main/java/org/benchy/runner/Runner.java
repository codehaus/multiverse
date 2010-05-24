package org.benchy.runner;

import com.google.gson.*;
import org.benchy.Benchmark;
import org.benchy.TestCase;
import org.benchy.repository.BenchmarkResultRepository;
import org.benchy.repository.FileBasedBenchmarkResultRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author Peter Veentjer
 */
public class Runner {

    public static void main(String[] args) throws Exception {
        System.out.println("Benchy: Starting the benchmark runner");

        RunnerArguments arguments = createArguments(args);

        BenchmarkResultRepository repository = loadRepository(arguments.repository);

        Benchmark benchmark = loadBenchmark(readbenchmarkJson());

        BenchmarkRunner runner = new DefaultBenchmarkRunner(repository);
        runner.execute(benchmark);

        System.out.println("Benchy: Finished");
    }

    private static RunnerArguments createArguments(String[] args) {
        //System.out.println("---------------------------------");
        //for(String s: args){
        //    System.out.println("arg: "+s);
        //}
        //System.out.println("---------------------------------");

        RunnerArguments cli = new RunnerArguments();
        //CmdLineParser parser = new CmdLineParser(cli);
        //try {
        //parser.parseArgument(args);
        return cli;
        //} catch (CmdLineException e) {
        //    System.err.println(e.getMessage());
        //    parser.printUsage(System.out);
        //    System.exit(-1);
        //    return null;
        //}
    }

    private static String readbenchmarkJson() throws IOException {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = stdin.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private static BenchmarkResultRepository loadRepository(String path) {
        File storedResultsRootDir = new File(path);
        System.out.printf("Benchy: Storing benchmark result in '%s'\n", path);
        return new FileBasedBenchmarkResultRepository(storedResultsRootDir);
    }

    private static Benchmark loadBenchmark(String benchmarkJson) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Benchmark.class, new BenchmarkDeserializer());
        builder.registerTypeAdapter(TestCase.class, new TestCaseDeserializer());
        Gson gson = builder.create();
        return gson.fromJson(benchmarkJson, Benchmark.class);
    }

    static class BenchmarkDeserializer implements JsonDeserializer {

        @Override
        public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            Benchmark benchmark = new Benchmark();

            JsonObject obj = (JsonObject) json;
            benchmark.setBenchmarkName(obj.get("benchmarkName").getAsString());
            benchmark.setDriverClass(obj.get("driverClass").getAsString());

            JsonArray testCaseJsonArray = obj.get("testcases").getAsJsonArray();
            for (JsonElement element : testCaseJsonArray) {
                JsonObject o = (JsonObject) element;
                TestCase testCase = context.deserialize(o, TestCase.class);
                benchmark.getTestCases().add(testCase);
            }

            return benchmark;
        }
    }

    static class TestCaseDeserializer implements JsonDeserializer {
        @Override
        public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = (JsonObject) json;

            TestCase testCase = new TestCase();
            for (Map.Entry<String, JsonElement> s : obj.entrySet()) {
                testCase.setProperty(s.getKey(), s.getValue().getAsString());
            }
            return testCase;
        }
    }
}
