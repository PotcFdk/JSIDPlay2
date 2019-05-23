package ui.common;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

public class ImageQueue {

	private static final int MAX_SIZE = 100;

	private final List<Image> imageQueue = new ArrayList<>(150);

	public synchronized void add(Image image) {
		if (imageQueue.size() >= MAX_SIZE) {
			// prevent OutOfMemoryError, just in case!
			if (!imageQueue.removeAll(imageQueue)) {
				throw new RuntimeException("ImageQueue cannot be cleared!");
			}
		}
		imageQueue.add(image);
	}

	public synchronized Image get() {
		// if we run out of sync, prevent image buffer overflow by dropping frames
		int size = imageQueue.size() / 10;
		for (int i = 0; size > 1 && i < imageQueue.size(); i += imageQueue.size() / size) {
			imageQueue.remove(i);
		}
		if (imageQueue.isEmpty()) {
			return null;
		}
		return imageQueue.remove(0);
	}

}
