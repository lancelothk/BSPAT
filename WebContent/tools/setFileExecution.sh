#!/bin/bash  
cd ./bismark
chmod +x bismark bismark_genome_preparation methylation_extractor 
cd ../bowtie
chmod +x bowtie bowtie-debug bowtie-build bowtie-build-debug bowtie-inspect bowtie-inspect-debug 
cd ../liftover
chmod +x liftOver
