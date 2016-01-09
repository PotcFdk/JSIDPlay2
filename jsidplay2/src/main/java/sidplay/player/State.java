package sidplay.player;

public enum State {
	/** Player about to start to play a tune */
	START,
	/** Player playing */
	PLAY,
	/** Player paused */
	PAUSE,
	/** Player temporarily stopped and is about to be reconfigured */
	STOP,
	/** Player ended, because the play time is over */
	END,
	/** Player restarts after playing a tune */
	RESTART,
	/** Player was quit */
	QUIT
}