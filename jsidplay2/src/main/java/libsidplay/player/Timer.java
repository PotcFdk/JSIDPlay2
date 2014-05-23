package libsidplay.player;

import libsidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IConfig;

/**
 * The timer contains the start and end time of a currently played song. It
 * notifies about reaching the start and end time by calling start/stop methods.
 */
public abstract class Timer {
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

	/**
	 * Timer start time in seconds.
	 */
	private long start;

	/**
	 * Timer end in seconds
	 */
	private long end;

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
		updateEnd();
	}

	/**
	 * Update timer end.
	 * <UL>
	 * <LI>SLDB enabled and song length well known? Use song length
	 * <LI>default length? Use default length relative to start
	 * <LI>default length == 0? Play forever
	 * </UL>
	 */
	public final void updateEnd() {
		final IConfig config = player.getConfig();
		final SidTune tune = player.getTune();
		// cancel last stop time event
		cancel(endTimeEvent);
		// default play default length or forever (0) ...
		end = config.getSidplay2().getDefaultPlayLength();
		if (end != 0) {
			// use default length (is meant to be relative to start)
			end = schedule(start + end, endTimeEvent);
		}
		if (tune != null && config.getSidplay2().isEnableDatabase()) {
			int songLength = player.getSidDatabaseInfo(db -> db.length(tune));
			if (songLength > 0) {
				// ... or use song length of song length database
				end = schedule(songLength, endTimeEvent);
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
			// event is in the past? Trigger immediately!
			eventScheduler.scheduleAbsolute(event, 0, Phase.PHI1);
		} else {
			eventScheduler.scheduleAbsolute(event, absoluteCycles, Phase.PHI1);
		}
		return seconds;
	}

	/**
	 * Cancel event.
	 */
	private void cancel(Event event) {
		EventScheduler eventScheduler = player.getC64().getEventScheduler();
		eventScheduler.cancel(event);
	}
	
	public long getEnd() {
		return end;
	}

	public abstract void start();

	public abstract void end();
}