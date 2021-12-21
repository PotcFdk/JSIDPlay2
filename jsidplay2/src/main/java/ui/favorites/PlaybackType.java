package ui.favorites;

public enum PlaybackType {
	PLAYBACK_OFF,
	/**
	 * Play tune after another within one favorites list (normal mode)
	 */
	NORMAL,
	/**
	 * Play tune after another within one favorites list and repeat if list is ended
	 * (repeated mode)
	 */
	REPEATED,
	/**
	 * Play random tunes within one favorites list
	 */
	RANDOM_ONE,
	/**
	 * Play random tunes within ALL favorites list
	 */
	RANDOM_ALL,
	/**
	 * Play random tunes within HVSC
	 */
	RANDOM_HVSC
}
