#!/bin/bash
# $1 is blat query path
# $2 is ref version
# $3 is ref path
# $4 is ref file name
perl $1/BlatBot_updated.pl Human $2 DNA chrom,start,score $3/$4 hyperlink $4.link
perl $1/parseBlatOutput_updated.pl hyperlink $4.link > $4.link.psl

