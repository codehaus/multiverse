#!/bin/bash

function runbenchmark(){
java -javaagent:~/.m2/repository/org/multiverse/multiverse-alpha/0.4-SNAPSHOT/multiverse-alpha-0.4-SNAPSHOT-jar-with-dependencies.jar \
 -classpath ~/.m2/repository/org/multiverse/multiverse-benchy/0.4-SNAPSHOT/multiverse-benchy-0.4-SNAPSHOT.jar \
 org.benchy.executor.BenchmarkExecutorMain ~/benchmarks <<< $1 EOF
}

#function createDiagram(){
#java -classpath target/classes/test:lib/support/* org.benchy.graph.GraphMain ~/benchmarks ../target/out.dat $1 $2 $3
#}

runbenchmark '{
    "benchmarkName":"baseline/queue/LinkedBlockingQueue",
    "driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedLinkedBlockingQueueDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "count":"2000000", "capacity":"1000"}
	]}'


mkdir -p ../target/diagrams
#createDiagram 'baseline/queue/LinkedBlockingQueue' 'threadCount' 'transactions/second'
#gnuplot <<< '
#set title "LinkedBlockingQueue"
#set xlabel "threads"
#set ylabel "transactions/second"
#set grid
#set terminal png
#set output "../target/diagrams/linkedblockingqueue.png"
#plot "../target/out.dat" using 1:2 title "LinkedBlockingQueue" with linespoint'

