#!/bin/sh
java -version
java -XX:+UnlockExperimentalVMOptions -XX:+UseCompressedOops -server -da -cp out/production/snzi:lib/junit-4.8.1.jar \
    org.multiverse.benchmarks.UncontendedReadScalabilityTest 1000000000
