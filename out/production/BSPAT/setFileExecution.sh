#!/bin/bash  
cd ./bismark
chmod u+x bismark bismark_genome_preparation methylation_extractor 
cd ../bowtie
chmod u+x bowtie bowtie-debug bowtie-build bowtie-build-debug bowtie-inspect bowtie-inspect-debug 
cd ../liftover
chmod u+x liftOver
cd ../BlatQuery
chmod u+x *
