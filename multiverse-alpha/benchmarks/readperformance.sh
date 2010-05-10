#!/bin/bash

. functions.sh

runbenchmark '{
    "benchmarkName":"ReadPerformance",
    "driverClass":"org.multiverse.benchmarks.ReadPerformanceDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"2","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"4","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"6","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"8","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"12","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"16","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"24","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"32","readonly":"true", "count":"2000000", "readCountPerThread":"100000000"}

	]}'


mkdir -p ../target/diagrams
createDiagram 'ReadPerformance' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "ReadPerformance"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "../target/diagrams/ReadPerformance.png"
plot "../target/out.dat" using 1:2 title "ReadPerformance" with linespoint'

