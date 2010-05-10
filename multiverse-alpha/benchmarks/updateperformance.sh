#!/bin/bash

. functions.sh

runbenchmark '{
    "benchmarkName":"NonConcurrentUpdate",
    "driverClass":"org.multiverse.benchmarks.NonConcurrentUpdateDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"12", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"16", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"24", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"32", "incCountPerThread":"10000000"}
	]}'


mkdir -p ../target/diagrams
createDiagram 'NonConcurrentUpdate' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "NonConcurrentUpdate"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "../target/diagrams/NonConcurrentUpdate.png"
plot "../target/out.dat" using 1:2 title "NonConcurrentUpdate" with linespoint'

