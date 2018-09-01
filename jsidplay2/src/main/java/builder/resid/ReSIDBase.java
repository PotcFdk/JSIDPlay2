package builder.resid;

import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDChip;
import libsidplay.common.SIDEmu;

public abstract class ReSIDBase extends SIDEmu {

	private static final Logger RESID = Logger.getLogger(ReSIDBase.class.getName());

	private final SIDChip sid = createSID();

	private EventScheduler context;
	
	/**
	 * Consumes samples of the SID while clocking.
	 */
	private IntConsumer sampler;

	/**
	 * Last time chip was accessed.
	 */
	protected long lastTime;

	/**
	 * Constructor
	 *
	 * @param context
	 *            {@link EventScheduler} context to use.
	 */
	public ReSIDBase(EventScheduler context) {
		this.context = context;
		reset((byte) 0xf);
	}

	void setSampler(IntConsumer sampler) {
		this.sampler = sampler;
	}

	IntConsumer getSampler() {
		return sampler;
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
	}

	@Override
	public byte read(int addr) {
		clock();
		return sid.read(addr);
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		sid.write(addr, data);
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}
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
	public void input(int input) {
		sid.input(input);
	}

	@Override
	public int getInputDigiBoost() {
		return sid.getInputDigiBoost();
	}

	public abstract byte readENV(int voiceNum);

	public abstract byte readOSC(int voiceNum);

	protected abstract SIDChip createSID();

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

}
