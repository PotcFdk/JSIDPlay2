/**
 *                   Event scheduler (based on alarm from Vice)
 *                   ------------------------------------------
 *  begin                : Wed May 9 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package libsidplay.common;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.Event.Phase;

/**
 * Fast EventScheduler, which maintains a linked list of Events. This scheduler
 * takes neglible time even when it is used to schedule events for nearly every
 * clock.
 * 
 * Events occur on an internal clock which is 2x the visible clock. The visible
 * clock is divided to two phases called phi1 and phi2.
 * 
 * The phi1 clocks are used by VIC and CIA chips, phi2 clocks by CPU.
 * 
 * Scheduling an event for a phi1 clock when system is in phi2 causes the event
 * to be moved to the next phi1 cycle. Correspondingly, requesting a phi1 time
 * when system is in phi2 returns the value of the next phi1.
 * 
 * @author Antti S. Lankila
 */
public final class EventScheduler {
	/** EventScheduler's current clock */
	private long currentTime;

	private double cyclesPerSecond;

	private List<Event> threadSafeQueue = new ArrayList<Event>();
	private List<Event> threadSafeOscilloscopeQueue = new ArrayList<Event>();

	/**
	 * The tail event, always after every other event.
	 */
	private final Event lastEvent = new Event("Tail") {
		{
			triggerTime = Long.MAX_VALUE;
		}

		@Override
		public void event() {
			throw new RuntimeException(
					"Event scheduler ran out of events to execute");
		}
	};

	/**
	 * The root of event chain, always before and after every other event.
	 */
	private final Event firstEvent = new Event("Root") {
		{
			triggerTime = Long.MIN_VALUE;
		}

		@Override
		public void event() {
			throw new RuntimeException(
					"Event scheduler executed the root event");
		}
	};

	/**
	 * Periodic thread-safe event scheduling mechanism.
	 */
	private final Event threadSafeQueueingEvent = new Event(
			"Inject events in thread-safe manner.") {
		@Override
		public void event() throws InterruptedException {
			synchronized (threadSafeQueue) {
				if (!threadSafeQueue.isEmpty()) {
					threadSafeQueue.remove(0).event();
				}
			}
			schedule(this, 50000);
		}
	};

	/**
	 * Periodic thread-safe event scheduling mechanism for Oscilloscope view.
	 * Needs to be scheduled periodically very often to measure sample audio
	 * provided by SID. Queue contains always one event, only. if Oscilloscope
	 * is unused, the queue is emptied and the event is canceled again.
	 */
	private final Event threadSafeOscilloscopeQueueingEvent = new Event(
			"Inject Oscilloscope event in thread-safe manner.") {
		@Override
		public void event() throws InterruptedException {
			synchronized (threadSafeOscilloscopeQueue) {
				if (!threadSafeOscilloscopeQueue.isEmpty()) {
					threadSafeOscilloscopeQueue.get(0).event();
				}
			}
			schedule(this, 128);
		}
	};

	/**
	 * Schedule an event in a thread-safe manner.
	 * 
	 * The thread-safe queue is moved to the unsafe queue periodically, and
	 * specific execution time is unpredictable, but will always occur during
	 * the PHI1 phase.
	 * 
	 * @param event
	 *            The event to schedule.
	 */
	public void scheduleThreadSafe(final Event event) {
		synchronized (threadSafeQueue) {
			threadSafeQueue.add(event);
		}
	}

	/**
	 * Schedule an event in a thread-safe manner for Oscilloscope.
	 * 
	 * The thread-safe queue is moved to the unsafe queue periodically, and
	 * specific execution time is every 128 cycles as specified in the
	 * threadSafeOscilloscopeQueueingEvent and will always occur during the PHI2
	 * phase. The queue is always cleared before adding the event.
	 * 
	 * @param event
	 *            The event to schedule.
	 */
	public void scheduleOscilloscopeThreadSafe(final Event event) {
		cancel(threadSafeOscilloscopeQueueingEvent);
		synchronized (threadSafeOscilloscopeQueue) {
			threadSafeOscilloscopeQueue.clear();
			threadSafeOscilloscopeQueue.add(event);
		}
		schedule(threadSafeOscilloscopeQueueingEvent, 0, Event.Phase.PHI2);
	}

	/**
	 * Clear Oscilloscope event.
	 */
	public void cancelOscilloscopeThreadSafe() {
		cancel(threadSafeOscilloscopeQueueingEvent);
	}

	public EventScheduler() {
		reset();
	}

	/**
	 * Add event to pending queue.
	 * 
	 * At PHI2, specify cycles=0 and Phase=PHI1 to fire on the very next PHI1.
	 * 
	 * @param event
	 *            The event to add
	 * @param cycles
	 *            How many cycles from now to fire
	 * @param phase
	 *            The phase when to fire the event.
	 */
	public void schedule(final Event event, final long cycles,
			final Event.Phase phase) {
		// this strange formulation always selects the next available slot
		// regardless of specified phase.
		event.triggerTime = (cycles << 1) + currentTime
				+ (currentTime & 1 ^ (phase == Event.Phase.PHI1 ? 0 : 1));
		addEventToSchedule(event);
	}

	/**
	 * Add event to pending queue in the same phase as current event.
	 * 
	 * @param event
	 *            The event to add
	 * @param cycles
	 *            How many cycles from now to fire.
	 */
	public void schedule(final Event event, final long cycles) {
		event.triggerTime = (cycles << 1) + currentTime;
		addEventToSchedule(event);
	}

	/**
	 * Schedule event to occur at some absolute time.
	 * 
	 * @param event
	 *            The event to add
	 * @param absoluteCycles
	 *            When to fire
	 * @param phase
	 *            Phase when event fires
	 */
	public void scheduleAbsolute(Event event, long absoluteCycles, Phase phase) {
		event.triggerTime = (absoluteCycles << 1)
				+ (phase == Phase.PHI2 ? 1 : 0);
		addEventToSchedule(event);
	}

	/**
	 * Scan the event queue and schedule event for execution.
	 * 
	 * @param event
	 *            The event to add
	 */
	private void addEventToSchedule(final Event event) {
		Event scan = firstEvent;
		while (true) {
			final Event next = scan.next;
			/* find the right spot where to tuck this new event */
			if (next.triggerTime > event.triggerTime) {
				event.next = next;
				scan.next = event;
				break;
			}

			scan = next;
		}
	}

	/**
	 * Cancel the specified event.
	 * 
	 * @param event
	 *            The event to cancel
	 * @return true if an event was actually removed
	 */
	public boolean cancel(final Event event) {
		Event prev = firstEvent;
		Event scan = firstEvent.next;

		while (scan != lastEvent) {
			if (event == scan) {
				prev.next = scan.next;
				return true;
			}

			prev = scan;
			scan = scan.next;
		}

		return false;
	}

	/** Cancel all pending events and reset time. */
	public void reset() {
		threadSafeQueue.clear();
		threadSafeOscilloscopeQueue.clear();
		currentTime = 0;
		firstEvent.next = lastEvent;
		schedule(threadSafeQueueingEvent, 0, Event.Phase.PHI1);
	}

	/**
	 * Fire next event, advance system time to that event
	 * 
	 * @throws InterruptedException
	 */
	public void clock() throws InterruptedException {
		final Event event = firstEvent.next;
		firstEvent.next = event.next;
		currentTime = event.triggerTime;
		event.event();
	}

	/**
	 * Is the event pending in this scheduler?
	 * 
	 * @param event
	 *            the event
	 * @return true when pending
	 */
	public boolean isPending(final Event event) {
		Event scan = firstEvent.next;
		while (scan != lastEvent) {
			if (event == scan) {
				return true;
			}
			scan = scan.next;
		}
		return false;
	}

	/**
	 * Get time with respect to a specific clock phase
	 * 
	 * @param phase
	 *            The phase
	 * @return the time according to specified phase.
	 */
	public long getTime(final Event.Phase phase) {
		return currentTime + (phase == Event.Phase.PHI1 ? 1 : 0) >> 1;
	}

	/**
	 * Return current clock phase
	 * 
	 * @return The current phase
	 */
	public Event.Phase phase() {
		return (currentTime & 1) == 0 ? Event.Phase.PHI1 : Event.Phase.PHI2;
	}

	public double getCyclesPerSecond() {
		return cyclesPerSecond;
	}

	public void setCyclesPerSecond(double cyclesPerSecond) {
		this.cyclesPerSecond = cyclesPerSecond;
	}
}
