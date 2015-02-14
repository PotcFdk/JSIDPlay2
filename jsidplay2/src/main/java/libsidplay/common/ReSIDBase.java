package libsidplay.common;


public abstract class ReSIDBase extends SIDEmu {

	/**
	 * Current position that audio is being written to.
	 */
	protected int bufferpos;

	/**
	 * Audio output sample buffer.
	 */
	protected int[] buffer;

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 * @param mixerEvent
	 *            {@link MixerEvent} to use.
	 */
	public ReSIDBase(EventScheduler context, final int bufferSize) {
		super(context);
		buffer = new int[bufferSize];
		bufferpos = 0;
	}

}
