package jsidplay2;

import jsidplay2.dirs.SidDirs;
import jsidplay2.photos.SidAuthors;

public class Photos {

	public static byte[] getPhoto(String collectionName, String author) {
		byte[] imageData = null;
		if (collectionName != null) {
			imageData = SidDirs.getDirectoryImageData(collectionName);
		}
		if (imageData == null && author != null) {
			imageData = SidAuthors.getImageData(author);
		}
		return imageData != null ? imageData : SidDirs.getUnknownImageData();
	}

}
