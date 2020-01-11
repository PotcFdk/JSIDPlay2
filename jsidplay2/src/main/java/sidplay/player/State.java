package sidplay.player;

/**
 * SID player state.
 * 
 * @author ken
 *
 */
public enum State {
	/** Player is about to start to play a tune */
	OPEN,
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