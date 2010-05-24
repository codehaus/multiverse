#!/bin/bash

export version="0.6-SNAPSHOT"

function runbenchmark(){
    java -classpath ../../multiverse-benchy/target/multiverse-benchy-$version.jar:../target/test-classes/:../target/classes \
        org.benchy.runner.Runner <<< $1 EOF
}

function createDiagram(){
    java -classpath ../../multiverse-benchy/target/multiverse-benchy-$version.jar org.benchy.graph.GraphMain ~/benchmarks ../target/out.dat $1 $2 $3
}
