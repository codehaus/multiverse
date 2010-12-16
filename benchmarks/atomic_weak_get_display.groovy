import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import static org.benchy.JGraphGraphBuilder.*

def benchmarks = searcher.findAllBenchmarks('atomic_weak_get')
println("Benchy > Found ${benchmarks.size()} results")

def categoryDataSet = new DefaultCategoryDataset();
def xySeriesDataSet = new XYSeriesCollection();

for (def benchmark in benchmarks) {
    def series = new XYSeries(benchmark.date);

    def entries = new LinkedList(benchmark.testCases)
    entries.sort {it.threadCount}

    entries.each {
        def testCase = it
        def transactionsPerSecondPerThread = testCase.average('transactionsPerSecondPerThread')
        categoryDataSet.addValue(transactionsPerSecondPerThread, benchmark.date, testCase.threadCount)
        series.add(testCase.threadCount, transactionsPerSecondPerThread)
    }

    xySeriesDataSet.addSeries(series);
}

writeLineChartAsPng(xySeriesDataSet, "Atomic Get", "threads", "transaction/second/thread", new File("charts/atomic_weak_get_line_wide.png"))
writeLineChartAsPng(xySeriesDataSet, "Atomic Get", "threads", "transaction/second/thread", 600,new File("charts/atomic_weak_get_line_narrow.png"))
writeBarChartAsPng(categoryDataSet, "Atomic Get", "threads", "transaction/second/thread", new File("charts/atomic_weak_get_bar.png"))


