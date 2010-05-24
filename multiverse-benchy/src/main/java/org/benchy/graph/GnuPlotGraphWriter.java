package org.benchy.graph;

import org.benchy.TestCaseResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * The {@link GraphWriter} that exports in gnu-plot format.
 *
 * @author Peter Veentjer.
 */
public class GnuPlotGraphWriter implements GraphWriter {

    private File outputFile;
    private String keyName;
    private String valueName;

    public GnuPlotGraphWriter(File outputFile, String keyName, String valueName) {
        this.outputFile = outputFile;
        this.keyName = keyName;
        this.valueName = valueName;
    }

    @Override
    public void write(GraphModel graphModel) {
        TreeMap<Integer, List<Pair>> treeMap = new TreeMap<Integer, List<Pair>>();

        for (String lineId : graphModel.getLineIds()) {
            List<TestCaseResult> propertyList = graphModel.get(lineId);
            for (TestCaseResult properies : propertyList) {

                String x = properies.get(keyName);
                String y = properies.get(valueName);

                List<Pair> valueList = treeMap.get(new Integer(x));
                if (valueList == null) {
                    valueList = new LinkedList<Pair>();
                    treeMap.put(new Integer(x), valueList);
                }

                valueList.add(new Pair(lineId, y));
            }
        }

        StringBuffer sb = new StringBuffer();
        for (Integer x : treeMap.descendingKeySet()) {
            sb.append(x);

            for (String lineId : graphModel.getLineIds()) {
                String result = "";
                for (Pair pair : treeMap.get(x)) {
                    if (pair.getLineId().equals(lineId)) {
                        result = pair.getValue();
                    }
                }

                sb.append(" " + result);
            }
            sb.append("\n");
        }

        write(sb.toString());
    }

    private void write(String s) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            out.write(s);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Pair {
        private final String lineId;
        private final String value;

        public Pair(String lineId, String value) {
            this.lineId = lineId;
            this.value = value;
        }

        public String getLineId() {
            return lineId;
        }

        public String getValue() {
            return value;
        }
    }
}
