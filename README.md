BSPAT
=====

BSPAT is a fast online service for analysing co-occurrence methylation patterns in bisulfite sequencing data. BSPAT integrated sequence mapping, quality control and visualized analysis result of co-occurrence DNA methylation patterns. Besides, it is much faster than existing tools which are based on multiple sequence alignment and pair-wise sequence alignment algorithms.

Website: http://cbc.case.edu/BSPAT

## Deployment

Download war file from releases. Then just deploy the war inside Apache Tomcat. 
The configuration file "config.properties" locate in BSPAT/web/WEB-INF. You can set server email account and bowtie and bismark paths. Also there is a switch for single/multiple thread mode here.

### Notice
There are many sleep() calls in bismark code to increase the readability during run time, which can be safely commented. Since BSPAT call bismark a lot of times, those sleep() calls will tremendously increase the running time of BSPAT. It is stongly recommended to comment those sleep() calls in the bismark code which will be used by BSPAT.

## bismark/bowtie compatibility
Currently bismark_0.14.2 and bowtie1.1.1 are tested and supported. Since changes of bismark/bowtie may introduce issues in BSPAT, it is not guaranteed BSPAT can work correctly under other version of bisamrk/bowtie.
E.g. Between bismark_0.10.1 and bismark_0.14.2, default output of bismark changed from sam to bam. To use sam as output, additional option --sam is needed. 
            
## Known Issues:            

Please note that BSPAT use font "Arial" as default when generating text in figures. The font used in figure can be configured in config.properties. Use different font may cause figure components alignment issue.

## Citation
Hu, K., Ting, A.H. and Li, J., 2015. BSPAT: a fast online tool for DNA methylation co-occurrence pattern analysis based on high-throughput bisulfite sequencing data. BMC bioinformatics, 16(1), p.1.

## License

Copyright 2014 © Computational Biology lab @ Case Western Reserve University.
See the LICENSE file for license rights and limitations (GPL v3).
