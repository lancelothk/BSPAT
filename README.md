BSPAT
=====

BSPAT is a fast online service for analysing co-occurrence methylation patterns in bisulfite sequencing data. BSPAT integrated sequence mapping, quality control and visualized analysis result of co-occurrence DNA methylation patterns. Besides, it is much faster than existing tools which are based on multiple sequence alignment and pair-wise sequence alignment algorithms.

Website: http://cbc.case.edu/BSPAT

## Deployment

Clone the project in your local directory. Use Intellij Idea to load the project and build the war file. Then just deploy the war inside Apache Tomcat. Upon deployment, BSPAT will automatically set up the integrated Bismark and other tools.

To switch between single thread or multiple threads, just comment and uncomment the code in MappingServlet.java and AnalysisServlet.java as follows:

//			ExecutorService executor = Executors.newSingleThreadExecutor(); // single thread
            ExecutorService executor = Executors.newCachedThreadPool();// multiple threads

Please note that BSPAT use font "Arial" as default when generating text in figures. Without this font, the text in figure may align incorrectly.
How to fix:
1. Install "Arial" font in the deployed system.
2. Change to another font in DrawPattern.java, but may need to change other parameters to align text in figure correctly.

## License

Copyright 2014 © Computational Biology lab @ Case Western Reserve University.
See the LICENSE file for license rights and limitations (GPL v3).
