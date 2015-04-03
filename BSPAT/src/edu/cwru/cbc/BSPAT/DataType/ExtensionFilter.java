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
        // ignore case in name string
        String lowercaseName = name.toLowerCase();
        if (isSingle) {
            // ignore case in extension string
            return lowercaseName.endsWith(extension.toLowerCase());
        } else {
            for (String ext : exts) {
                // ignore case in extension string
                if (lowercaseName.endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

    }

}
