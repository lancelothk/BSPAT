#!/bin/bash
if [ $1 ]
then
    echo "start setting tools executable"
    cd $1/bismark
    chmod u+x bismark bismark_genome_preparation bismark2bedGraph bismark2report bismark_methylation_extractor coverage2cytosine deduplicate_bismark
    cd $1/bowtie
    chmod u+x bowtie bowtie-debug bowtie-build bowtie-build-debug bowtie-inspect bowtie-inspect-debug
    cd $1/liftover
    chmod u+x liftOver
    cd $1/BlatQuery
    chmod u+x BlatQuery.sh BlatBot_updated.pl parseBlatOutput_updated.pl
    echo "finished setting tools executable"
else
    echo "first parameter is null!"
fi