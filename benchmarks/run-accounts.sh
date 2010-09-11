#!/bin/sh
java -version

CLASSPATH="../build/classes/main:../build/classes/test:../lib/junit-4.8.1.jar"
VM_OPTIONS="-server -da -XX:+UnlockExperimentalVMOptions"
java -cp $CLASSPATH  $VM_OPTIONS org.multiverse.stms.beta.benchmarks.AccountBenchmark 1000000000
