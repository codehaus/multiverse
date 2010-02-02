#!/bin/bash

function runbenchmark(){
java -javaagent:target/multiverse-0.1.jar -classpath target/classes/test:lib/support/* org.benchy.executor.BenchmarkMain ~/benchmarks <<< $1 EOF
}

function createDiagram(){
java -classpath target/classes/test:lib/support/* org.benchy.graph.GraphMain ~/benchmarks /tmp/out.dat $1 $2 $3
}

runbenchmark '{
    "benchmarkName":"baseline/queue/LinkedBlockingQueue",
    "driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedLinkedBlockingQueueDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "count":"2000000", "capacity":"1000"}
	]}'

createDiagram 'baseline/queue/LinkedBlockingQueue' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "LinkedBlockingQueue"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "/tmp/linkedblockingqueue.png"
plot "/tmp/out.dat" using 1:2 title "LinkedBlockingQueue" with linespoint'

