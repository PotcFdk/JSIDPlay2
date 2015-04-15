package resid_builder;

import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDChip;
import libsidplay.common.SIDEmu;


public abstract class ReSIDBase extends SIDEmu {

	private static final Logger RESID = Logger.getLogger(ReSIDBase.class.getName());

	protected final SIDChip sid;

	/**
	 * Current position that audio is being written to.
	 */
	protected int bufferpos;

	/**
	 * Audio output sample buffer.
	 */
	protected final int[] buffer;

	/**
	 * Consumes samples of the SID while clocking.
	 */
	private IntConsumer sampler;
	
	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 * @param mixerEvent
	 *            {@link Mixer} to use.
	 */
	public ReSIDBase(EventScheduler context, final int bufferSize) {
		super(context);
		this.sid = createSID();
		this.buffer = new int[bufferSize];
		this.bufferpos = 0;
		sampler = sample -> {
			buffer[bufferpos++] = sample;
		};
		reset((byte) 0xf);
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		clock();
		return sid.read(addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		super.write(addr, data);
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}

		clock();
		sid.write(addr, data);
	}

	@Override
	public void clock() {
		int cycles = clocksSinceLastAccess();
		sid.clock(cycles, sampler);
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
		sid.mute(num, mute);
	}

	/**
	 * Sets the clock frequency.
	 *
	 * @param systemClock
	 *            System clock to use for the SID.
	 */
	@Override
	public void setClockFrequency(final double systemClock) {
		sid.setClockFrequency(systemClock);
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 */
	@Override
	public void setChipModel(final ChipModel model) {
		sid.setChipModel(model);
	}

	@Override
	public ChipModel getChipModel() {
		return sid.getChipModel();
	}

	@Override
	public void input(int input) {
		sid.input(input);
	}

	@Override
	public int getInputDigiBoost() {
		return sid.getInputDigiBoost();
	}

	public SIDChip getSID() {
		return sid;
	}

	protected abstract SIDChip createSID();
	
}
