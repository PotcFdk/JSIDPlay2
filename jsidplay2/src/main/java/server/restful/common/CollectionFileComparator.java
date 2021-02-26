package server.restful.common;

import java.io.File;
import java.util.Comparator;

public class CollectionFileComparator implements Comparator<File> {

	@Override
	public int compare(File file1, File file2) {
		if (file1.isDirectory() && file2.isFile()) {
			return -1;
		} else if (file1.isFile() && file2.isDirectory()) {
			return 1;
		} else {
			return file1.getName().toLowerCase().compareToIgnoreCase(file2.getName().toLowerCase());
		}
	}
}
