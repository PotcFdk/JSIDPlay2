package libsidplay;

public enum State {
	/** Player is currently playing a song */
	RUNNING,
	/** Player has been paused */
	PAUSED,
	/** Player has been temporarily stopped and is about to be reconfigured */
	STOPPED,
	/** Player has stopped, because the play time is over */
	EXIT,
	/** Player will be restarted to play next/previous sub-tune */
	RESTART,
	/** Player was quit */
	QUIT
}