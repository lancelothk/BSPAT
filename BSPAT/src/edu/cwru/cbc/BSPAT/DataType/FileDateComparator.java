package edu.cwru.cbc.BSPAT.DataType;

import java.io.File;
import java.util.Comparator;

public class FileDateComparator implements Comparator<File> {

    @Override
    public int compare(File file0, File file1) {
        return Long.valueOf(file0.lastModified()).compareTo(file1.lastModified());
    }

}
