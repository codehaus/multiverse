#!/bin/bash

function runbenchmark(){
java -Xbootclasspath/a:lib/provided/boot.jar -javaagent:target/multiverse-0.1.jar -classpath target/classes/test:lib/support/* org.benchy.executor.BenchmarkMain ~/benchmarks <<< $1 EOF
}

function createDiagram(){
java -classpath target/classes/test:lib/support/* org.benchy.graph.GraphMain ~/benchmarks /tmp/out.dat $1 $2 $3
}

runbenchmark '{"benchmarkName":"baseline/queue/LinkedBlockingQueue","driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedLinkedBlockingQueueDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "count":"2000000", "capacity":"1000"}
	]}'

#runbenchmark '{"benchmarkName":"baseline/queue/SynchronousQueue/unfair","driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedSynchronousQueueDriver",
#	"testcases":[
#	    {"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"5", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"7", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"9", "count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"10","count":"100", "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"11","count":"100",  "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"12","count":"100",  "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"13","count":"100",  "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"14","count":"100",  "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"15","count":"100",  "fair":"false"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"16","count":"100",  "fair":"false"}
#	]}'

#runbenchmark '{"benchmarkName":"baseline/queue/SynchronousQueue/fair","driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedSynchronousQueueDriver",
#	"testcases":[
#	    {"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"5", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"7", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"9", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"10", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"11", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"12", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"13", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"14", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"15", "count":"100000", "fair":"true"},
#        {"runCount":"1", "warmupRunCount":"1", "threadCount":"16", "count":"100000", "fair":"true"}
#	]}'

runbenchmark '{"benchmarkName":"baseline/queue/ArrayBlockingQueue/unfair","driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedArrayBlockingQueueDriver",
    "testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"5", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"7", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"9", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"10", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"11", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"12", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"13", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"14", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"15", "count":"1000000", "capacity":"1000", "fair":"false"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"16", "count":"1000000", "capacity":"1000", "fair":"false"}
	]}'

runbenchmark '{"benchmarkName":"baseline/queue/ArrayBlockingQueue/fair","driverClass":"org.multiverse.benchmarks.drivers.oldschool.queue.ContendedArrayBlockingQueueDriver",
    "testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1",  "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"5", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"7", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"9", "count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"10","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"11","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"12","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"13","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"14","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"15","count":"100000", "capacity":"1000", "fair":"true"},
        {"runCount":"1", "warmupRunCount":"1", "threadCount":"16","count":"100000", "capacity":"1000", "fair":"true"}
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

createDiagram 'baseline/queue/ArrayBlockingQueue/fair' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "fair ArrayBlockingQueue"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "/tmp/fairarrayblockingqueue.png"
plot "/tmp/out.dat" using 1:2 title "fair ArrayBlockingQueue" with linespoint'

createDiagram 'baseline/queue/ArrayBlockingQueue/unfair' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "unfair ArrayBlockingQueue"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "/tmp/unfairarrayblockingqueue.png"
plot "/tmp/out.dat" using 1:2 title "unfair ArrayBlockingQueue" with linespoint'

createDiagram 'baseline/queue/ArrayBlockingQueue/unfair;baseline/queue/ArrayBlockingQueue/fair' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "ArrayBlockingQueue fair vs unfair"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "/tmp/arrayblockingqueuefairvsunfair.png"
plot "/tmp/out.dat" using 1:2 title "unfair arrayblockingqueue" with linespoint,\
     "/tmp/out.dat" using 1:3 title "fair arrayblockingqueue" with linespoint'

createDiagram 'baseline/queue/ArrayBlockingQueue/unfair;baseline/queue/LinkedBlockingQueue' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "LinkedBlockingQueue vs unfair ArrayBlockingQueue"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "/tmp/linkedblockingqueuevsfairarrayblockingqueue.png"
plot "/tmp/out.dat" using 1:2 title "unfair ArrayBlockingQueue" with linespoint,\
     "/tmp/out.dat" using 1:3 title "LinkedBlockingQueue" with linespoint'

#createDiagram 'baseline/queue/SynchronousQueue/fair' 'threadCount' 'transactions/second'
#gnuplot <<< '
#set title "fair SynchronousQueue"
#set xlabel "threads"
#set ylabel "transactions/second"
#set grid
#set terminal png
#set output "/tmp/fairsynchronousqueue.png"
#plot "/tmp/out.dat" using 1:2 title "fair SynchronousQueue" with linespoint'
#
#createDiagram 'baseline/queue/SynchronousQueue/unfair' 'threadCount' 'transactions/second'
#gnuplot <<< '
#set title "unfair SynchronousQueue"
#set xlabel "threads"
#set ylabel "transactions/second"
#set grid
#set terminal png
#set output "/tmp/unfairsynchronousqueue.png"
#plot "/tmp/out.dat" using 1:2 title "unfair SynchronousQueue" with linespoint'
#
#createDiagram 'baseline/queue/SynchronousQueue/unfair,baseline/queue/SynchronousQueue/fair' 'threadCount' 'duration(ns)'
#gnuplot <<< '
#set title 'SynchronousQueue fair vs unfair'
#set xlabel "threads"
#set ylabel "duration(ns)"
#set grid
#set terminal png
#set output "/tmp/synchronousqueuefairvsunfair.png"
#plot "/tmp/out.dat" using 1:2 title "unfair SynchronousQueue" with linespoint,
#plot "/tmp/out.dat" using 1:3 title "fair SynchronousQueue" with linespoint''