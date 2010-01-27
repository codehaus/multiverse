package org.benchy.executor;

import com.google.gson.*;
import org.benchy.BenchmarkResultRepository;
import org.benchy.FileBasedBenchmarkResultRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author Peter Veentjer
 */
public class BenchmarkMain {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Benchy the Benchmark Executor");

        BenchmarkResultRepository repository = loadRepository(args[0]);

        Benchmark benchmark = loadBenchmark(readbenchmarkJson());

        BenchmarkExecutor executor = new BenchmarkExecutor(repository);
        executor.execute(benchmark);

        System.out.println("Finished");
    }

    private static String readbenchmarkJson() throws IOException {
        // Defines the standard input stream
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
            benchmark.benchmarkName = obj.get("benchmarkName").getAsString();
            benchmark.driverClass = obj.get("driverClass").getAsString();

            JsonArray testCaseJsonArray = obj.get("testcases").getAsJsonArray();
            for (JsonElement element : testCaseJsonArray) {
                JsonObject o = (JsonObject) element;
                TestCase testCase = context.deserialize(o, TestCase.class);
                benchmark.testCases.add(testCase);
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
