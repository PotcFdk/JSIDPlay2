/*
 * datasette.c - CBM cassette implementation.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *  Andreas Matthies <andreas.matthies@gmx.net>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.c1530;

import java.io.File;
import java.io.IOException;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;

/**
 * Implementation of a C1530 Datasette.
 * 
 * @author Ken Händel
 * 
 */
public abstract class Datasette {
	public enum DatasetteStatus {
		OFF, LOAD, SAVE
	}

	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Maximum counter value.
	 */
	private static final int MAX_COUNTER = 1000;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_D = 1.27e-5;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_R = 1.07e-2;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_V_PLAY = 4.76e-2;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_G = 0.525;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). At FF/REWIND,
	 * Datasette-counter makes ~4 rounds per second.
	 */
	private static final double DS_RPS_FAST = 4.00;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_C1 = DS_V_PLAY / DS_D / Math.PI;
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_C2 = DS_R * DS_R / (DS_D * DS_D);
	/**
	 * Counter is c=g*(sqrt(v*t/d*pi+r^2/d^2)-r/d). Some constants for the
	 * Datasette-Counter.
	 */
	private static final double DS_C3 = DS_R / DS_D;

	/**
	 * Tape image to attach/detach.
	 */
	private final TapeImage img = new TapeImage();

	/**
	 * Insert a tape image into the datasette. A previously attached tape is
	 * ejected first.
	 *
	 * @param tapeFile
	 *            filename of the tape image
	 * @throws IOException
	 *             cannot read tape
	 */
	public final void insertTape(final File tapeFile) throws IOException {
		ejectTape();
		img.imageAttach(this, tapeFile);
	}

	/**
	 * Eject tape image.
	 * 
	 * @throws IOException
	 *             cannot write tape image
	 */
	public final void ejectTape() throws IOException {
		img.imageDetach(this);
	}

	/**
	 * Return value of method fetchGap.
	 * 
	 * @author Ken Händel
	 * 
	 */
	protected static final class GapDir {
		/**
		 * Constructor.
		 * 
		 * @param g
		 *            gap
		 * @param d
		 *            direction
		 */
		protected GapDir(final long g, final int d) {
			this.gap = g;
			this.dir = d;
		}

		/**
		 * Gap.
		 */
		protected long gap;
		/**
		 * Direction.
		 */
		protected int dir;
	}

	/**
	 * How many cycles it takes to start the datasette motor.
	 */
	private static final int MOTOR_DELAY = 32000;
	/**
	 * Size of the temp. tape buffer to read from.
	 */
	private static final int TAP_BUFFER_LENGTH = 100000;

	/**
	 * At least every DATASETTE_MAX_GAP cycle there should be an alarm.
	 */
	private static final int DATASETTE_MAX_GAP = 100000;

	/**
	 * Controls of the datasette.
	 * 
	 * @author Ken Händel
	 * 
	 */
	public enum Control {
		/**
		 * Press stop on tape.
		 */
		STOP,
		/**
		 * Press play on tape.
		 */
		START,
		/**
		 * Press forward on tape.
		 */
		FORWARD,
		/**
		 * Press rewind on tape.
		 */
		REWIND,
		/**
		 * Press record on tape.
		 */
		RECORD,
		/**
		 * Datasette reset button (not included on the real thing).
		 */
		RESET,
		/**
		 * Reset counter.
		 */
		RESET_COUNTER
	}

	/**
	 * Which mode is activated (RECORD/START/...)?
	 */
	protected Control mode = Control.STOP;

	/**
	 * Attached TAP tape image.
	 */
	protected Tap currentImage;

	/**
	 * Buffer for the TAP.
	 */
	private final byte[] tapBuffer = new byte[TAP_BUFFER_LENGTH];

	/**
	 * Pointer and length of the tapBuffer.
	 */
	protected long nextTap, lastTap;

	/**
	 * Shall the datasette reset when the CPU does?
	 */
	private final boolean resetDatasetteWithMainCPU;

	/**
	 * How long to wait, if a zero occurs in the tap?
	 */
	private final int zeroGapDelay;

	/**
	 * Low/high wave indicator for C16 TAPs.
	 */
	private int fullwave;
	/**
	 * Low/high wave indicator for C16 TAPs.
	 */
	private long fullwaveGap;

	/**
	 * State of the datasette motor.
	 */
	protected boolean motor;

	/**
	 * Last time we have recorded a flux change.
	 */
	protected long lastWriteClk;

	/**
	 * Motor stop is delayed.
	 */
	private long motorStopClk;

	/**
	 * Event scheduler.
	 */
	private final EventScheduler context;

	/**
	 * Last datasette counter value.
	 */
	private int lastCounter;

	/**
	 * Datasette event.
	 */
	private final Event event;

	/**
	 * GAP handling.
	 */
	private long longGapPending;

	/**
	 * GAP handling.
	 */
	private long longGapElapsed;

	/**
	 * Last direction of the tape (0/+1/-1).
	 */
	private int lastDirection;

	/**
	 * Remember the reset of tape-counter.
	 */
	private int counterOffset;

	/**
	 * Constructor.
	 * 
	 * @param ctx
	 *            event context
	 */
	public Datasette(final EventScheduler ctx) {
		this.context = ctx;
		resetDatasetteWithMainCPU = true;
		zeroGapDelay = 0x4e20;
		event = new Event("Datasette") {

			@Override
			public void event() {
				try {
					readBit();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}

	/**
	 * Update datasette counter value.
	 */
	protected void updateCounter() {
		if (currentImage == null) {
			return;
		}
		currentImage.counter = (MAX_COUNTER - counterOffset + (int) (DS_G * (Math
				.sqrt(currentImage.cycleCounter / context.getCyclesPerSecond()
						* DS_C1 + DS_C2) - DS_C3)))
				% MAX_COUNTER;

		if (lastCounter != currentImage.counter) {
			lastCounter = currentImage.counter;
		}
	}

	/**
	 * Reset counter value.
	 */
	protected void resetCounter() {
		if (currentImage == null) {
			return;
		}
		counterOffset = (MAX_COUNTER + (int) (DS_G * (Math
				.sqrt(currentImage.cycleCounter / context.getCyclesPerSecond()
						* DS_C1 + DS_C2) - DS_C3)))
				% MAX_COUNTER;
		updateCounter();
	}

	/**
	 * reads buffer to fit the next gap-read tap_buffer[next_tap] ~
	 * currentFileSeekPosition.
	 * 
	 * @param offset
	 *            offset to move
	 * @return move succeeded
	 */
	private boolean moveBufferForward(final int offset) {
		if (nextTap + offset >= lastTap) {
			try {
				currentImage.fd.seek(currentImage.currentFilePosition
						+ currentImage.offset);
			} catch (IOException e) {
				System.err.println("Cannot read in tap-file.");
				return false;
			}
			try {
				lastTap = currentImage.fd.read(tapBuffer);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			nextTap = 0;
			if (nextTap >= lastTap) {
				return false;
			}
		}
		return true;
	}

	/**
	 * reads buffer to fit the next gap-read at current_file_seek_position-1
	 * tap_buffer[next_tap] ~ currentFileSeekPosition.
	 * 
	 * @param offset
	 *            offset to move
	 * @return move succeeded
	 */
	private boolean moveBufferBack(final int offset) {
		if (nextTap + offset < 0) {
			if (currentImage.currentFilePosition >= TAP_BUFFER_LENGTH) {
				nextTap = TAP_BUFFER_LENGTH;
			} else {
				nextTap = currentImage.currentFilePosition;
			}
			try {
				currentImage.fd.seek(currentImage.currentFilePosition - nextTap
						+ currentImage.offset);
			} catch (IOException e) {
				System.err.println("Cannot read in tap-file.");
				return false;
			}
			try {
				lastTap = currentImage.fd.read(tapBuffer);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			if (nextTap > lastTap) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Fetch GAP.
	 * 
	 * @param gd
	 *            IN/OUT parameters
	 * @param readTap
	 *            ?
	 * @return succeeded
	 */
	private boolean fetchGap(final GapDir gd, final long readTap) {
		if (readTap >= lastTap || readTap < 0) {
			return false;
		}
		gd.gap = tapBuffer[(int) readTap] & 0xff;

		if (currentImage.version == 0 || gd.gap != 0) {
			gd.gap <<= 3;
		} else {
			if (readTap >= lastTap - 3) {
				return false;
			}
			gd.dir *= 4;
			gd.gap = (tapBuffer[(int) (readTap + 1)] & 0xff)
					+ ((tapBuffer[(int) (readTap + 2)] & 0xff) << 8)
					+ ((tapBuffer[(int) (readTap + 3)] & 0xff) << 16);
		}

		if (0 == gd.gap) {
			gd.gap = zeroGapDelay;
		}

		return true;
	}

	/**
	 * Read GAP forward.
	 * 
	 * @return next GAP
	 */
	private long readGapForward() {
		return nextTap;
	}

	/**
	 * Read GAP backward (Tape version 0).
	 * 
	 * @return previous GAP
	 */
	private long readGapBackwardV0() {
		return nextTap - 1;
	}

	/**
	 * Read GAP backward (Tape version 1). Examine, if previous gap was long by
	 * rewinding until 3 non-zero-values in a row found, then reading forward
	 * 
	 * @param readTap
	 *            current tape position
	 * @return previous GAP
	 * @throws Exception
	 *             error moving buffer position
	 */
	private long readGapBackwardV1(final long readTap) throws Exception {
		long rememberFileSeekPosition = currentImage.currentFilePosition;

		currentImage.currentFilePosition -= 4;
		nextTap -= 4;

		int nonZerosInARow = 0;
		while (nonZerosInARow < 3 && currentImage.currentFilePosition != 0) {
			if (!moveBufferBack(-1)) {
				return readTap;
			}
			currentImage.currentFilePosition--;
			nextTap--;
			if (tapBuffer[(int) nextTap] != 0) {
				nonZerosInARow++;
			} else {
				nonZerosInARow = 0;
			}
		}

		/* now forward */
		while (currentImage.currentFilePosition < rememberFileSeekPosition - 4) {
			if (!moveBufferForward(1)) {
				throw new Exception();
			}
			if (tapBuffer[(int) nextTap] != 0) {
				currentImage.currentFilePosition++;
				nextTap++;
			} else {
				currentImage.currentFilePosition += 4;
				nextTap += 4;
			}
		}
		if (!moveBufferForward(4)) {
			throw new Exception();
		}
		long newReadTap = nextTap;
		nextTap += rememberFileSeekPosition - currentImage.currentFilePosition;
		currentImage.currentFilePosition = rememberFileSeekPosition;

		return newReadTap;
	}

	/**
	 * Read GAP.
	 * 
	 * @param direction
	 *            1: forward, -1: rewind
	 * @return GAP
	 */
	private long readGap(int direction) {
		long readTap = 0;
		long gap = 0;

		if (currentImage.system != 2) {
			if (direction < 0 && !moveBufferBack(direction * 4)) {
				return 0;
			}
			if (direction > 0 && !moveBufferForward(direction * 4)) {
				return 0;
			}
			if (direction > 0) {
				readTap = readGapForward();
			} else {
				if (currentImage.version == 0 || nextTap < 4
						|| 0 != tapBuffer[(int) (nextTap - 4)]) {
					readTap = readGapBackwardV0();
				} else {
					try {
						readTap = readGapBackwardV1(readTap);
					} catch (Exception e) {
						return 0;
					}
				}
			}
			GapDir gapDir = new GapDir(gap, direction);
			if (!fetchGap(gapDir, readTap)) {
				return 0;
			}
			gap = gapDir.gap;
			direction = gapDir.dir;
			nextTap += direction;
			currentImage.currentFilePosition += direction;
		}

		if (currentImage.system == 2 && currentImage.version == 1) {
			if (0 == fullwave) {
				if (direction < 0 && !moveBufferBack(direction * 4)) {
					return 0;
				}
				if (direction > 0 && !moveBufferForward(direction * 4)) {
					return 0;
				}
				if (direction > 0) {
					readTap = readGapForward();
				} else {
					if (currentImage.version == 0 || nextTap < 4
							|| 0 != tapBuffer[(int) (nextTap - 4)]) {
						readTap = readGapBackwardV0();
					} else {
						try {
							readTap = readGapBackwardV1(readTap);
						} catch (Exception e) {
							return 0;
						}
					}
				}
				GapDir gapDir = new GapDir(gap, direction);
				if (!fetchGap(gapDir, readTap)) {
					return 0;
				}
				gap = gapDir.gap;
				direction = gapDir.dir;

				fullwaveGap = gap;
				nextTap += direction;
				currentImage.currentFilePosition += direction;
			} else {
				gap = fullwaveGap;
			}
			fullwave ^= 1;
		} else if (currentImage.system == 2 && currentImage.version == 2) {
			if (direction < 0 && !moveBufferBack(direction * 4)) {
				return 0;
			}
			if (direction > 0 && !moveBufferForward(direction * 4)) {
				return 0;
			}
			if (direction > 0) {
				readTap = readGapForward();
			} else {
				if (currentImage.version == 0 || nextTap < 4
						|| 0 != tapBuffer[(int) (nextTap - 4)]) {
					readTap = readGapBackwardV0();
				} else {
					try {
						readTap = readGapBackwardV1(readTap);
					} catch (Exception e) {
						return 0;
					}
				}
			}
			GapDir gapDir = new GapDir(gap, direction);
			if (!fetchGap(gapDir, readTap)) {
				return 0;
			}
			gap = gapDir.gap;
			direction = gapDir.dir;
			gap *= 2;
			fullwave ^= 1;
			nextTap += direction;
			currentImage.currentFilePosition += direction;
		}
		return gap;
	}

	/**
	 * Read bit.
	 * 
	 * @throws IOException
	 *             tape image read error
	 */
	protected void readBit() throws IOException {
		double speedOfTape = DS_V_PLAY;
		int direction = 1;
		long gap;

		this.context.cancel(event);

		if (currentImage == null) {
			return;
		}

		/* check for delay of motor stop */
		if (motorStopClk > 0 && context.getTime(Phase.PHI1) >= motorStopClk) {
			motorStopClk = 0;
			motor = false;
		}

		if (!motor) {
			return;
		}
		switch (mode) {
		case START:
			direction = 1;
			speedOfTape = DS_V_PLAY;
			setFlag(0 == longGapPending);
			break;
		case FORWARD:
			direction = 1;
			speedOfTape = DS_RPS_FAST
					/ DS_G
					* Math.sqrt(4 * Math.PI * DS_D * DS_V_PLAY
							/ context.getCyclesPerSecond()
							* currentImage.cycleCounter + 4 * Math.PI * Math.PI
							* DS_R * DS_R);
			break;
		case REWIND:
			direction = -1;
			speedOfTape = DS_RPS_FAST
					/ DS_G
					* Math.sqrt(4
							* Math.PI
							* DS_D
							* DS_V_PLAY
							/ context.getCyclesPerSecond()
							* (currentImage.cycleCounterTotal - currentImage.cycleCounter)
							+ 4 * Math.PI * Math.PI * DS_R * DS_R);
			break;
		case RECORD:
		case STOP:
			return;
		default:
			System.err.println("Unknown datasette mode.");
			return;
		}

		if (direction + lastDirection == 0) {
			/*
			 * the direction changed; read the gap from file, but use use only
			 * the elapsed gap
			 */
			gap = readGap(direction);
			longGapPending = longGapElapsed;
			longGapElapsed = gap - longGapElapsed;
		}
		if (longGapPending != 0) {
			gap = longGapPending;
			longGapPending = 0;
		} else {
			gap = readGap(direction);
			if (gap != 0) {
				longGapElapsed = 0;
			}
		}
		if (0 == gap) {
			control(Control.STOP);
			return;
		}
		if (gap > DATASETTE_MAX_GAP) {
			longGapPending = gap - DATASETTE_MAX_GAP;
			gap = DATASETTE_MAX_GAP;
		}
		longGapElapsed += gap;
		lastDirection = direction;

		if (direction > 0) {
			currentImage.cycleCounter += gap;
		} else {
			currentImage.cycleCounter -= gap;
		}

		this.context.schedule(event, (long) (gap * DS_V_PLAY / speedOfTape));
		updateCounter();
	}

	/**
	 * Sets the TAP image to work with.
	 * 
	 * @param image
	 *            TAP image
	 * @throws IOException
	 *             tape image read error
	 */
	public final void setTapeImage(final Tap image) throws IOException {
		currentImage = image;
		lastTap = nextTap = 0;
		internalReset();

		if (image != null) {
			/* We need the length of tape for realistic counter. */
			currentImage.cycleCounterTotal = 0;
			long gap;
			do {
				gap = readGap(1);
				currentImage.cycleCounterTotal += gap;
			} while (gap != 0);
			currentImage.currentFilePosition = 0;
			lastTap = nextTap = 0;
			fullwave = 0;
		}
	}

	/**
	 * Fast forward on tape.
	 */
	protected void forward() {
		if (this.context.isPending(event)) {
			this.context.cancel(event);
		}
		this.context.schedule(event, MAX_COUNTER);
	}

	/**
	 * Rewind on tape.
	 */
	protected void rewind() {
		if (this.context.isPending(event)) {
			this.context.cancel(event);
		}
		this.context.schedule(event, MAX_COUNTER);
	}

	/**
	 * Internal reset datasette.
	 * 
	 * @throws IOException
	 *             error reading TAP image
	 */
	protected void internalReset() throws IOException {
		if (currentImage != null) {
			if (mode == Control.START || mode == Control.FORWARD
					|| mode == Control.REWIND) {
				this.context.cancel(event);
			}
			control(Control.STOP);
			currentImage.seekStart();
			currentImage.cycleCounter = 0;
			counterOffset = 0;
			longGapPending = 0;
			longGapElapsed = 0;
			lastDirection = 0;
			motorStopClk = 0;
			updateCounter();
			fullwave = 0;
			lastCounter = -1;
		}
	}

	/**
	 * Reset datasette.
	 */
	public final void reset() {
		if (resetDatasetteWithMainCPU) {
			try {
				internalReset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start datasette motor.
	 * 
	 * @throws IOException
	 *             tape image read error
	 */
	protected void startMotor() throws IOException {
		currentImage.fd.seek(currentImage.currentFilePosition
				+ currentImage.offset);
		if (!context.isPending(event)) {
			this.context.schedule(event, MOTOR_DELAY);
		}
	}

	/**
	 * Triggered by GUI: RECORD/START...
	 * 
	 * @param command
	 *            triggered datasette control
	 */
	public final void control(final Control command) {
		if (currentImage == null) {
			return;
		}

		context.scheduleThreadSafe(new Event("Datasette command") {
			@Override
			public void event() {
				try {
					switch (command) {
					case RESET_COUNTER:
						resetCounter();
						break;
					case RESET:
						internalReset();
						// $FALL-THROUGH$
					case STOP:
						mode = Control.STOP;
						lastWriteClk = 0;
						break;
					case START:
						mode = Control.START;
						lastWriteClk = 0;
						if (motor) {
							startMotor();
						}
						break;
					case FORWARD:
						mode = Control.FORWARD;
						forward();
						lastWriteClk = 0;
						if (motor) {
							startMotor();
						}
						break;
					case REWIND:
						mode = Control.REWIND;
						rewind();
						lastWriteClk = 0;
						if (motor) {
							startMotor();
						}
						break;
					case RECORD:
						if (!currentImage.isReadOnly()) {
							mode = Control.RECORD;
							lastWriteClk = 0;
						}
						break;
					default:
						System.err.println("Unknown datasette mode.");
						break;
					}

					/* clear the tap-buffer */
					lastTap = nextTap = 0;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Triggered by c64pla_config_changed.
	 * 
	 * @param flag
	 *            motor on/off
	 */
	public final void setMotor(final boolean flag) {
		if (currentImage != null) {
			if (flag) {
				/* abort pending motor stop */
				motorStopClk = 0;
				if (!motor) {
					lastWriteClk = 0;
					try {
						startMotor();
					} catch (IOException e) {
						e.printStackTrace();
					}
					motor = true;
				}
			}
			if (!flag && motor && motorStopClk == 0) {
				motorStopClk = context.getTime(Phase.PHI1) + MOTOR_DELAY;
				if (!context.isPending(event)) {
					this.context.scheduleAbsolute(event, motorStopClk,
							Phase.PHI1);
				}
			}
		}
	}

	/**
	 * Write bit.
	 * 
	 * @throws IOException
	 *             tape image write error
	 */
	private void bitWrite() throws IOException {
		long clk = context.getTime(Phase.PHI1);

		long writeTime = clk - lastWriteClk;
		if (writeTime < 8) {
			return;
		}

		if (writeTime < 256 << 3) {
			/* keep bottom 3 bits of lastWriteClk to avoid clock skew */
			lastWriteClk += writeTime & 0x7f8;
			byte writeGap = (byte) (writeTime >> 3);
			try {
				currentImage.fd.write(writeGap);
			} catch (IOException e) {
				control(Control.STOP);
				return;
			}
			currentImage.currentFilePosition++;
		} else {
			lastWriteClk = clk;

			/*
			 * write long gap designation, or just a placeholder for a long gap,
			 * if we are version 0.
			 */
			try {
				currentImage.fd.write((byte) 0);
			} catch (IOException e) {
				control(Control.STOP);
				return;
			}
			currentImage.currentFilePosition++;

			/* write exact gap length for modern images */
			if (currentImage.version >= 1) {
				byte[] longGap = new byte[3];
				longGap[0] = (byte) (writeTime & 0xff);
				longGap[1] = (byte) (writeTime >> 8 & 0xff);
				longGap[2] = (byte) (writeTime >> 16 & 0xff);
				try {
					currentImage.fd.write(longGap);
				} catch (IOException e) {
					control(Control.STOP);
					return;
				}
				currentImage.currentFilePosition += longGap.length;
			}
		}
		if (currentImage.size < currentImage.currentFilePosition) {
			currentImage.size = currentImage.currentFilePosition;
		}
		currentImage.cycleCounter += writeTime;

		if (currentImage.cycleCounterTotal < currentImage.cycleCounter) {
			currentImage.cycleCounterTotal = currentImage.cycleCounter;
		}
		currentImage.hasChanged = true;
		updateCounter();
	}

	/**
	 * Triggered by CPU port update at PHI2.
	 * 
	 * @param writeBit
	 *            toggle state
	 */
	public final void toggleWriteBit(final boolean writeBit) {
		if (currentImage != null && writeBit && mode == Control.RECORD) {
			if (motor) {
				if (lastWriteClk == 0) {
					lastWriteClk = context.getTime(Phase.PHI2);
				} else {
					try {
						bitWrite();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public boolean getMotor() {
		return motor;
	}

	public int getCounter() {
		if (currentImage == null) {
			return 0;
		}
		return currentImage.counter;
	}

	public int getProgress() {
		if (!motor) {
			// No motor activity,
			// signal finish
			return 100;
		}
		if (currentImage == null || currentImage.cycleCounterTotal == 0) {
			return 0;
		}
		return (int) (((float) currentImage.cycleCounter / currentImage.cycleCounterTotal) * 100);
	}

	/**
	 * Read state of button press.
	 * 
	 * @return The read state of a button press.
	 */
	public boolean getTapeSense() {
		return mode != Control.STOP;
	}

	/**
	 * Signal interrupt flag.
	 * 
	 * @param flag
	 *            interrupt flag
	 */
	public abstract void setFlag(final boolean flag);

	/**
	 * Get a status icon to display the floppies activity.
	 * 
	 * @return icon to show
	 */
	public DatasetteStatus getStatus() {
		if (motor && mode == Control.START) {
			return DatasetteStatus.LOAD;
		} else if (motor && mode == Control.RECORD) {
			return DatasetteStatus.SAVE;
		} else {
			return DatasetteStatus.OFF;
		}
	}

	public TapeImage getTapeImage() {
		return img;
	}
}
