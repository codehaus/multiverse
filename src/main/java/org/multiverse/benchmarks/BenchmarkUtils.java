package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.Ref;

import java.text.NumberFormat;
import java.util.*;

public class BenchmarkUtils {

    public static void loadOtherTransactionalObjectClasses() {
        Ref ref = new Ref();
        ref.openForConstruction(new BetaObjectPool());

        //todo: better loading.. stress transaction
        LongRef longRef = new LongRef(0);
        IntRef floatRef = new IntRef(0);
    }


    public static void startAll(Thread... threads) {
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public static void joinAll(Thread... threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int[] generateProcessorRange() {
        return generateProcessorRange(Runtime.getRuntime().availableProcessors());
    }

    public static int[] generateProcessorRange(int maxProcessors) {
        List<Integer> list = new LinkedList<Integer>();

        for (int k = 1; k <= 16; k++) {
            if (k <= maxProcessors) {
                list.add(k);
            }
        }

        list.add(maxProcessors);

        int k = 16;
        while (k < maxProcessors) {
            k = (int) (k * 1.07);
            if (k <= maxProcessors) {
                list.add(k);
            }
        }

        //remove all bigger than maxProcessors
        for (Integer value : list) {
            if (value > maxProcessors) {
                list.remove(value);
            }
        }

        //remove all duplicates
        list = new LinkedList(new HashSet(list));
        //sort them
        Collections.sort(list);

        Integer[] integerArray = new Integer[list.size()];
        integerArray = list.toArray(integerArray);

        int[] result = new int[integerArray.length];
        for (int l = 0; l < integerArray.length; l++) {
            result[l] = integerArray[l];
        }

        return result;
    }

    public static void toGnuplot(Result[] result) {
        println("---------------------------------------------");
        println("------------------ GNUPLOT ------------------");
        println("---------------------------------------------");
        println("set terminal png");
        println("set output \"result.png\"");
        println("set xlabel \"threads\"");
        println("set origin 0,0");
        println("set ylabel \"transactions/second\"");
        println("plot '-' using 1:2 with lines");
        for (Result aResult : result) {
            println("%s %s", aResult.processorCount, aResult.performance);
        }
        println("e");
        println("");
    }

    public static void println(String s, Object... args) {
        System.out.printf(s + "\n", args);
    }


    public static String transactionsPerSecond(long count, long timeMs) {
        double performance = (1000 * count) / timeMs;
        return format(performance);
    }

    public static double perSecond(long transactionsPerThread, long totalTimeMs, int threads) {
        long transactionCount = transactionsPerThread * threads * threads;
        return (1000d * transactionCount) / totalTimeMs;
    }

    public static String format(double value) {
        return NumberFormat.getInstance(Locale.US).format(value);
    }
}