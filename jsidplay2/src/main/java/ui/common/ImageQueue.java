package ui.common;

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
public final class ImageQueue<T> {

	private static class QueueItem<T> {
		private T image;
		private QueueItem<T> next;
	}

	private static final int MAX_SIZE = 60;

	private QueueItem<T> head, tail;
	private int size;
	private boolean disposed;

	public final synchronized void add(T image) {
		if (disposed) {
			return;
		}
		// prevent overflow
		if (size == MAX_SIZE) {
			head = head.next;
			size--;
		}
		QueueItem<T> item = new QueueItem<>();
		item.image = image;

		if (tail == null) {
			head = item;
			tail = head;
		} else {
			tail.next = item;
			tail = item;
		}
		size++;
	}

	public final synchronized T poll() {
		// prevent overflow by dropping in-between frames
		if (size > 20) {
			QueueItem<T> prev = head;
			QueueItem<T> scan = head;
			int i = 0;
			while (i < size) {
				int j = 0;
				while (j < 10 && i + j < size) {
					prev = scan;
					scan = scan.next;
					j++;
				}
				i += j;
				if (i < size) {
					prev.next = scan.next;
					if (prev.next == null) {
						tail = prev;
					}
					size--;
				}
			}
		}
		if (head == null) {
			return null;
		}
		if (head == tail) {
			tail = null;
		}
		T result = head.image;
		head = head.next;
		size--;
		return result;
	}

	public final synchronized void clear() {
		head = tail = null;
		size = 0;
	}

	public final synchronized void dispose() {
		clear();
		disposed = true;
	}

}
