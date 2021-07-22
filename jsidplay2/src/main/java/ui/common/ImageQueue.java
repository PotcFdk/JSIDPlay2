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
		private final T image;
		private QueueItem<T> next;

		private QueueItem(T image) {
			this.image = image;
		}
	}

	private static final int MAX_QUEUE_SIZE = 60;
	private static final int DROP_NTH_FRAME = 10;

	private QueueItem<T> head, tail;
	private int size;
	private boolean disposed;

	public final synchronized void push(T image) {
		if (disposed) {
			return;
		}
		// prevent overflow, replace first frame
		if (size == MAX_QUEUE_SIZE) {
			head = head.next;
			size--;
		}
		QueueItem<T> item = new QueueItem<>(image);
		if (tail == null) {
			// empty list? image is the first
			head = item;
			tail = head;
		} else {
			tail.next = item;
			tail = item;
		}
		size++;
	}

	public final synchronized T pull() {
		// prevent overflow by dropping in-between frames
		int count = size / DROP_NTH_FRAME;
		if (count > 1) {
			QueueItem<T> prev = head, scan = head;
			while (count-- > 0) {
				// skip frames
				for (int i = 0; i < DROP_NTH_FRAME && scan != tail; i++) {
					prev = scan;
					scan = scan.next;
				}
				if (scan == tail) {
					// end of list? We remove the last frame
					tail = prev;
				}
				// remove in-between frame
				prev.next = scan.next;
				size--;
			}
		}
		if (tail == null) {
			// empty list? Nothing to poll
			return null;
		}
		if (head == tail) {
			// One sized list? Mark list as empty
			tail = null;
		}
		// poll image from queue
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
