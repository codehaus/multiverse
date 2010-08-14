#!/bin/sh
java -version
java -XX:+UseParallelGC -XX:+UseParallelOldGC -server -da \
    -cp out/production/snzi:lib/junit-4.8.1.jar org.multiverse.benchmarks.UncontendedUpdateScalabilityTest  250000000
