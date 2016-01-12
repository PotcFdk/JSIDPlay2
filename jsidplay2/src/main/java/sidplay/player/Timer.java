package sidplay.player;

import sidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.config.IConfig;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;

/**
 * The timer contains the start and end time of a currently played song. It
 * notifies about reaching the start and end time by calling start/stop methods.
 * Additionally the fade-in and fade-out start time notification has been added.
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

	final Event fadeInStartTimeEvent = new Event("Fade-in Start") {
		@Override
		public void event() throws InterruptedException {
			fadeInStart(fadeIn);
		}
	};

	final Event fadeOutStartTimeEvent = new Event("Fade-out Start") {
		@Override
		public void event() throws InterruptedException {
			fadeOutStart(fadeOut);
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
	 * Fade-in time in seconds (0 means no fade-in).
	 */
	private int fadeIn;

	/**
	 * Fade-out time in seconds (0 means no fade-out).
	 */
	private int fadeOut;

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
		final IConfig config = player.getConfig();
		fadeIn = config.getSidplay2Section().getFadeInTime();
		fadeOut = config.getSidplay2Section().getFadeOutTime();
		schedule(start, startTimeEvent);
		if (fadeIn != 0) {
			schedule(start, fadeInStartTimeEvent);
		}
		updateEnd();
	}

	/**
	 * Update timer end.
	 * <UL>
	 * <LI>MP3 tune? We always play forever
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
		if (fadeOut != 0) {
			cancel(fadeOutStartTimeEvent);
		}
		// MP3 tune length is undetermined, therefore we always play forever
		if (tune instanceof MP3Tune) {
			return;
		}
		// Only for tunes: check song length
		if (tune != SidTune.RESET
				&& config.getSidplay2Section().isEnableDatabase()) {
			int songLength = player.getSidDatabaseInfo(
					db -> db.getSongLength(tune), 0);
			if (songLength > 0) {
				// use song length of song length database ...
				end = schedule(songLength, endTimeEvent);
				if (fadeOut != 0) {
					schedule(end - fadeOut, fadeOutStartTimeEvent);
				}
				return;
			}
		}
		// ... or play default length (0 means forever)
		end = config.getSidplay2Section().getDefaultPlayLength();
		if (end != 0) {
			// use default length (is meant to be relative to start)
			end = schedule(start + end, endTimeEvent);
			if (fadeOut != 0) {
				schedule(end - fadeOut, fadeOutStartTimeEvent);
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
		if (absoluteCycles < eventScheduler.getTime(Phase.PHI1)) {
			// event is in the past? Trigger immediately!
			eventScheduler.scheduleAbsolute(event, 0, Phase.PHI1);
		} else {
			// event is in the future
			eventScheduler.scheduleAbsolute(event, absoluteCycles, Phase.PHI1);
		}
		return seconds;
	}

	/**
	 * Cancel event.
	 */
	private void cancel(Event event) {
		player.getC64().getEventScheduler().cancel(event);
	}

	public long getEnd() {
		return end;
	}

	public abstract void start();

	public abstract void end();

	public abstract void fadeInStart(int fadeIn);

	public abstract void fadeOutStart(int fadeOut);
}