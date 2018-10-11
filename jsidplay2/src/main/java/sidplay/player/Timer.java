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
	/**
	 * Tune start time has been reached
	 */
	private Event startTimeEvent = new Event("Timer Start") {
		@Override
		public void event() throws InterruptedException {
			start();
		}
	};

	/**
	 * Tune end time has been reached
	 */
	private Event endTimeEvent = new Event("Timer End") {
		@Override
		public void event() throws InterruptedException {
			end();
		}
	};

	/**
	 * Tune fade-in time has been reached
	 */
	private Event fadeInStartTimeEvent = new Event("Fade-in Start") {
		@Override
		public void event() throws InterruptedException {
			fadeInStart(fadeIn);
		}
	};

	/**
	 * Tune fade-out time has been reached
	 */
	private Event fadeOutStartTimeEvent = new Event("Fade-out Start") {
		@Override
		public void event() throws InterruptedException {
			fadeOutStart(fadeOut);
		}
	};

	/**
	 * Timer start time in seconds.
	 */
	private double start;

	/**
	 * Timer end in seconds
	 */
	private double end;

	/**
	 * Fade-in time in seconds (0 means no fade-in).
	 */
	private double fadeIn;

	/**
	 * Fade-out time in seconds (0 means no fade-out).
	 */
	private double fadeOut;

	/**
	 * The player.
	 */
	private final Player player;

	/**
	 * Create a song length timer for the player
	 * 
	 * @param player
	 *            SID player
	 */
	public Timer(final Player player) {
		this.player = player;
	}

	/**
	 * Set tune start time
	 * 
	 * @param start
	 *            start time
	 */
	public final void setStart(final double start) {
		this.start = start;
	}

	/**
	 * Reset timer events
	 */
	public final void reset() {
		final IConfig config = player.getConfig();
		fadeIn = config.getSidplay2Section().getFadeInTime();
		fadeOut = config.getSidplay2Section().getFadeOutTime();
		schedule(start, startTimeEvent, false);
		if (fadeIn != 0) {
			schedule(start, fadeInStartTimeEvent, false);
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
		if (tune != SidTune.RESET && config.getSidplay2Section().isEnableDatabase()) {
			double songLength = player.getSidDatabaseInfo(db -> db.getSongLength(tune), 0.);
			if (songLength > 0) {
				// use song length of song length database ...
				end = schedule(songLength, endTimeEvent, true);
				if (fadeOut != 0) {
					schedule(end - fadeOut, fadeOutStartTimeEvent, true);
				}
				return;
			}
		}
		// ... or play default length (0 means forever)
		end = config.getSidplay2Section().getDefaultPlayLength();
		if (end > 0) {
			// use default length (is meant to be relative to start)
			end = schedule(start + end, endTimeEvent, true);
			if (fadeOut != 0) {
				schedule(start + end - fadeOut, fadeOutStartTimeEvent, true);
			}
		}
	}

	/**
	 * Schedule start or end timer event.<BR>
	 * Note: If the event is in the past: trigger immediately
	 * 
	 * @param seconds
	 *            absolute schedule time in seconds
	 * @param event
	 *            timer event to schedule
	 */
	private double schedule(final double seconds, final Event event, final boolean includeEndpoint) {
		double maxSeconds = seconds + (includeEndpoint ? .001 : 0);
		EventScheduler eventScheduler = player.getC64().getEventScheduler();
		double cyclesPerSecond = eventScheduler.getCyclesPerSecond();
		long absoluteCycles = (long) (maxSeconds * cyclesPerSecond);
		if (absoluteCycles < eventScheduler.getTime(Phase.PHI1)) {
			// event is in the past? Trigger immediately!
			eventScheduler.scheduleAbsolute(event, 0, Phase.PHI1);
		} else {
			// event is in the future
			eventScheduler.scheduleAbsolute(event, absoluteCycles, Phase.PHI1);
		}
		return maxSeconds;
	}

	/**
	 * Cancel event.
	 * 
	 * @param timer
	 *            event to cancel
	 */
	private void cancel(final Event event) {
		player.getC64().getEventScheduler().cancel(event);
	}

	/**
	 * Get tune end time in cycles
	 * 
	 * @return tune end time
	 */
	public final double getEnd() {
		return end;
	}

	/**
	 * Notification of tune start
	 */
	public abstract void start();

	/**
	 * Notification of tune end
	 */
	public abstract void end();

	/**
	 * Notification of tune fade-in start
	 */
	public abstract void fadeInStart(double fadeIn);

	/**
	 * Notification of tune fade-out start
	 */
	public abstract void fadeOutStart(double fadeOut);
}