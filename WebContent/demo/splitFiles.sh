#!/bin/bash
head -n 100 $1 > demoSequence_$2.fastq
tail -n +101 $1 > tmp
mv tmp $1
