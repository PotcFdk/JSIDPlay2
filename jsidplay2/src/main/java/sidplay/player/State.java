package sidplay.player;

public enum State {
	/** Player about to start to play a tune */
	START,
	/** Player playing */
	PLAY,
	/** Player paused */
	PAUSE,
	/** Player ended, because the play time is over */
	END,
	/** Player restarts the same tune */
	RESTART,
	/** Player was quit */
	QUIT
}