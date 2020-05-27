package ui.common;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

/**
 * ImageQueue implements a queue like data structure. Frame images are queued
 * for every video frame the emulation produces. Frames are then polled with the
 * screen refresh rate 50Hz/60Hz to show them on the video screen.
 *
 * <B>Note:</B> We must prevent OutOfMemoryError in case nobody polls the
 * frames.
 *
 * <B>Note:</B> If we run out of sync between emulation-time and real-time, we
 * throw away intermediate frames.
 *
 * @author ken
 *
 */
public class ImageQueue {

	private static final int MAX_SIZE = 100;

	private final List<Image> imageQueue = new ArrayList<>(MAX_SIZE);

	private boolean disposed;

	public synchronized void add(Image image) {
		if (disposed) {
			return;
		}
		if (imageQueue.size() == MAX_SIZE) {
			// prevent OutOfMemoryError, just in case!
			imageQueue.remove(0);
		}
		imageQueue.add(image);
	}

	public synchronized Image poll() {
		// if we run out of sync, prevent overflow by dropping in-between frames
		for (int i = 0; imageQueue.size() > 20 && i < imageQueue.size(); i += 10) {
			imageQueue.remove(i);
		}
		if (imageQueue.isEmpty()) {
			return null;
		}
		return imageQueue.remove(0);
	}

	public synchronized void clear() {
		imageQueue.clear();
	}

	public synchronized void dispose() {
		clear();
		disposed = true;
	}

}
