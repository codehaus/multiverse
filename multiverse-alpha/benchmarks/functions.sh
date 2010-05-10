#!/bin/bash

function runbenchmark(){
    java -javaagent:../target/multiverse-alpha-0.6-SNAPSHOT.jar \
        -classpath ../../multiverse-benchy/target/multiverse-benchy-0.6-SNAPSHOT.jar:../target/test-classes/ \
        org.benchy.executor.BenchmarkExecutorMain ~/benchmarks <<< $1 EOF
}

function createDiagram(){
    java -classpath ../../multiverse-benchy/target/multiverse-benchy-0.6-SNAPSHOT.jar org.benchy.graph.GraphMain ~/benchmarks ../target/out.dat $1 $2 $3
}
