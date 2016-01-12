package sidplay.player;

public enum State {
	/** Player starts to play a tune */
	START,
	/** Player playing */
	PLAY,
	/** Player paused */
	PAUSE,
	/** Player ended, because the play time is over */
	END,
	/** Player restarts the same tune */
	RESTART,
	/** Player quit */
	QUIT
}