package edu.cwru.cbc.BSPAT.DataType;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilter implements FilenameFilter {
    private String extension;
    private String[] exts;
    private boolean isSingle;

    public ExtensionFilter(String extension) {
        this.extension = extension;
        this.isSingle = true;
    }

    public ExtensionFilter(String[] exts) {
        this.exts = exts;
        this.isSingle = false;
    }

    @Override
    public boolean accept(File dir, String name) {
        String lowercaseName = name.toLowerCase();
        if (isSingle) {
            if (lowercaseName.endsWith(extension)) {
                return true;
            } else {
                return false;
            }
        } else {
            for (String ext : exts) {
                if (lowercaseName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }

    }

}
