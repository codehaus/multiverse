package org.benchy.graph;

import org.benchy.TestCaseResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GraphModel {

    private Map<String, List<TestCaseResult>> map = new HashMap<String, List<TestCaseResult>>();
    private List<String> lineIdList = new LinkedList<String>();

    public void add(String lineId, TestCaseResult testCaseResult) {
        List<TestCaseResult> list = map.get(lineId);
        if (list == null) {
            list = new LinkedList<TestCaseResult>();
            map.put(lineId, list);
            lineIdList.add(lineId);
        }

        list.add(testCaseResult);
    }

    public List<String> getLineIds() {
        return Collections.unmodifiableList(lineIdList);
    }

    public List<TestCaseResult> get(String lineId) {
        return map.get(lineId);
    }
}
