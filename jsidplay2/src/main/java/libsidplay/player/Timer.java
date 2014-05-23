package libsidplay.player;

import libsidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IConfig;

/**
 * The timer contains the start and length of a currently played song. It
 * notifies about reaching the start and end of a song.
 */
public abstract class Timer {

	/**
	 * Timer start time in seconds.
	 */
	private long start;

	/**
	 * Timer length in seconds
	 */
	private long length;

	/**
	 * The player.
	 */
	private final Player player;

	public Timer(final Player player) {
		this.player = player;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public final void reset() {
		schedule(start, startTimeEvent);
		updateLength();
	}

	/**
	 * Update timer length.
	 * <UL>
	 * <LI>SLDB enabled and song length well known -> use song length
	 * <LI>default length -> use default length relative to start
	 * <LI>default length == 0 -> play forever
	 * </UL>
	 */
	public final void updateLength() {
		final IConfig config = player.getConfig();
		final SidTune tune = player.getTune();
		// default play default length or forever (0) ...
		length = config.getSidplay2().getDefaultPlayLength();
		if (length != 0) {
			// default length is relative to start
			length = schedule(start + length, endTimeEvent);
		}
		if (tune != null && config.getSidplay2().isEnableDatabase()) {
			int tuneLength = player.getSidDatabaseInfo(db -> db.length(tune));
			if (tuneLength > 0) {
				// ... or use song length of song length database
				length = schedule(tuneLength, endTimeEvent);
			}
		}
	}

	/**
	 * Schedule start or end timer event.<BR>
	 * Note: If the event is in the past: trigger immediately
	 */
	private long schedule(long seconds, Event event) {
		EventScheduler eventScheduler = player.getC64().getEventScheduler();
		double cyclesPerSecond = eventScheduler.getCyclesPerSecond();
		long absoluteCycles = (long) (seconds * cyclesPerSecond);
		eventScheduler.cancel(event);
		if (absoluteCycles < eventScheduler.getTime(Phase.PHI1)) {
			// event is in the past
			eventScheduler.scheduleAbsolute(event, 0, Phase.PHI1);
		} else {
			eventScheduler.scheduleAbsolute(event, absoluteCycles, Phase.PHI1);
		}
		return seconds;
	}

	public long getLength() {
		return length;
	}

	final Event startTimeEvent = new Event("Timer Start") {
		@Override
		public void event() throws InterruptedException {
			start();
		}
	};

	final Event endTimeEvent = new Event("Timer End") {
		@Override
		public void event() throws InterruptedException {
			end();
		}
	};

	public abstract void start();

	public abstract void end();
}