#!/bin/bash

. functions.sh

runbenchmark '{
    "benchmarkName":"ProgrammaticConcurrentUpdate",
    "driverClass":"org.multiverse.benchmarks.programmatic.ConcurrentUpdateDriver",
	"testcases":[
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"1", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"2", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"3", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"4", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"6", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"8", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"12", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"16", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"24", "count":"2000000", "incCountPerThread":"10000000"},
		{"runCount":"1", "warmupRunCount":"1", "threadCount":"32", "count":"2000000", "incCountPerThread":"10000000"}
	]}'


mkdir -p ../target/diagrams
createDiagram 'ProgrammaticConcurrentUpdate' 'threadCount' 'transactions/second'
gnuplot <<< '
set title "ProgrammaticConcurrentUpdate"
set xlabel "threads"
set ylabel "transactions/second"
set grid
set terminal png
set output "../target/diagrams/ProgrammaticConcurrentUpdate.png"
plot "../target/out.dat" using 1:2 title "ProgrammaticLong ConcurrentUpdate" with linespoint'

