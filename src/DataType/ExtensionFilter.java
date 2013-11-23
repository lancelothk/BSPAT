package DataType;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilter implements FilenameFilter {
	private String extension;
	private String[] exts;
	private boolean isSingle;

	public ExtensionFilter(String extension) {
		// TODO Auto-generated constructor stub
		this.extension = extension;
		this.isSingle = true;
	}

	public ExtensionFilter(String[] exts) {
		// TODO Auto-generated constructor stub
		this.exts = exts;
		this.isSingle = false;
	}

	@Override
	public boolean accept(File dir, String name) {
		// TODO Auto-generated method stub
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
