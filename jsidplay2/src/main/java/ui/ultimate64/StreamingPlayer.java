package ui.ultimate64;

import static sidplay.player.State.PLAY;
import static sidplay.player.State.QUIT;
import static sidplay.player.State.START;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import sidplay.player.ObjectProperty;
import sidplay.player.State;

public abstract class StreamingPlayer {
	private static final int QUIT_MAX_WAIT_TIME = 1000;

	private ObjectProperty<State> stateProperty = new ObjectProperty<State>(State.class.getSimpleName(), QUIT);

	private Thread streamingThread;

	private Runnable streamingRunnable = () -> {
		try {
			open();
			stateProperty.set(START);
			stateProperty.set(PLAY);
			while (stateProperty.get() == State.PLAY) {
				play();
			}
		} catch (InterruptedException | IOException | LineUnavailableException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			close();
		}
	};

	protected abstract void open() throws IOException, LineUnavailableException;

	protected abstract void play() throws IOException, InterruptedException;

	protected abstract void close();

	public void start() {
		if (streamingThread == null || !streamingThread.isAlive()) {
			streamingThread = new Thread(streamingRunnable, "Player");
			streamingThread.setPriority(Thread.MAX_PRIORITY);
			streamingThread.start();
		}
	}

	public void stop() {
		try {
			while (streamingThread != null && streamingThread.isAlive()) {
				stateProperty.set(QUIT);
				streamingThread.join(QUIT_MAX_WAIT_TIME);
			}
		} catch (InterruptedException e) {
		}
	}
}
