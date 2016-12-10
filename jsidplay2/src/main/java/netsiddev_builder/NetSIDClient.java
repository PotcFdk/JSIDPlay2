package netsiddev_builder;

import static netsiddev.Response.BUSY;
import static netsiddev.Response.OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.util.Pair;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.components.pla.PLA;
import libsidplay.config.IEmulationSection;
import netsiddev.Response;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.GetConfigCount;
import netsiddev_builder.commands.GetConfigInfo;
import netsiddev_builder.commands.GetVersion;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.NetSIDPkg;
import netsiddev_builder.commands.SetClocking;
import netsiddev_builder.commands.SetSidLevel;
import netsiddev_builder.commands.SetSidPosition;
import netsiddev_builder.commands.TryDelay;
import netsiddev_builder.commands.TryRead;
import netsiddev_builder.commands.TryReset;
import netsiddev_builder.commands.TrySetSampling;
import netsiddev_builder.commands.TrySetSidCount;
import netsiddev_builder.commands.TrySetSidModel;
import netsiddev_builder.commands.TryWrite;

public class NetSIDClient {
	private static final int CYCLES_TO_MILLIS = 1000;
	private static final int MAX_WRITE_CYCLES = 4096;
	private static final int MAX_BUFFER_SIZE = 4096;
	private static final int BUFFER_NEAR_FULL = MAX_WRITE_CYCLES * 3 >> 2;
	private static final int REGULAR_DELAY = MAX_WRITE_CYCLES >> 2;

	private NetSIDDevConnection connection = NetSIDDevConnection.getInstance();

	private static Map<Pair<ChipModel, String>, Byte> FILTER_TO_SID_MODEL = new HashMap<>();

	private byte version;
	private EventScheduler context;
	private List<NetSIDPkg> commands = new ArrayList<>(MAX_BUFFER_SIZE);
	private TryWrite tryWrite = new TryWrite();
	private byte readResult;
	private String configName;
	private long lastSIDWriteTime;
	private int fastForwardFactor;
	private boolean startTimeReached;

	private final Event event = new Event("JSIDDevice Delay") {

		@Override
		public void event() {
			// XXX we just delay and clock first SID for reading, enough?
			context.schedule(event, eventuallyDelay((byte) 0), Event.Phase.PHI2);
		}
	};

	/**
	 * Establish a single instance connection to a NetworkSIDDevice.
	 * 
	 * Always MAX_SIDS are reserved.
	 * 
	 * @param context
	 *            event context
	 */
	public NetSIDClient(EventScheduler context, IEmulationSection emulationSection) {
		this.context = context;
		try {
			connection.open(emulationSection.getNetSIDDevHost(), emulationSection.getNetSIDDevPort());
		} catch (IOException e) {
			connection.close();
			throw new RuntimeException(e);
		}
		version = getNetworkProtocolVersion();
		// Get all available SID models
		FILTER_TO_SID_MODEL.clear();
		for (byte config = 0; config < getConfigCount(); config++) {
			Pair<ChipModel, String> filter = getConfigInfo(config);
			FILTER_TO_SID_MODEL.put(new Pair<>(filter.getKey(), filter.getValue()), config);
		}
		addSetSidModels();
	}

	/**
	 * @param model
	 *            chip model
	 * @return sorted filter names of the desired chip model (case-insensitive)
	 */
	public static List<String> getFilterNames(ChipModel model) {
		return FILTER_TO_SID_MODEL.keySet().stream().filter(p -> p.getKey() == model).map(p -> p.getValue())
				.sorted((s1, s2) -> s1.compareToIgnoreCase(s2)).collect(Collectors.toList());
	}

	/**
	 * Add setting all SidModels to the first available configuration to the
	 * command queue without soft flush.
	 */
	private void addSetSidModels() {
		commands.add(new TrySetSidCount((byte) PLA.MAX_SIDS));
		for (byte sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			commands.add(new TrySetSidModel(sidNum, (byte) 0));
		}
	}

	/**
	 * Add setting all voice mutes to the command queue without soft flush.
	 */
	void addMutes(IEmulationSection emulationSection) {
		for (byte sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			for (byte voice = 0; voice < (version < 3 ? 3 : 4); voice++) {
				commands.add(new Mute(sidNum, voice, emulationSection.isMuteVoice(sidNum, voice)));
			}
		}
	}

	/**
	 * Lookup SID configuration of the desired name and configure
	 * NetworkSIDDevice accordingly.
	 * 
	 * @param sidNum
	 *            SID chip number
	 * @param chipModel
	 *            SID chip model
	 * @param filterName
	 *            desired filter name
	 */
	void setFilter(byte sidNum, final ChipModel chipModel, final String filterName) {
		send(() -> {
			Optional<Pair<ChipModel, String>> filter = FILTER_TO_SID_MODEL.keySet().stream()
					.filter(p -> p.getKey().equals(chipModel) && p.getValue().equals(filterName)).findFirst();
			if (filter.isPresent()) {
				return new TrySetSidModel(sidNum, FILTER_TO_SID_MODEL.get(filter.get()));
			}
			return new TrySetSidModel(sidNum, (byte) 0);
		});
	}

	private byte getNetworkProtocolVersion() {
		return sendReceive(() -> new GetVersion());
	}

	private byte getConfigCount() {
		return sendReceive(() -> new GetConfigCount());
	}

	private Pair<ChipModel, String> getConfigInfo(byte sidNum) {
		return sendReceiveConfig(() -> new GetConfigInfo(sidNum));
	}

	public void setClockFrequency(double cpuFrequency) {
		send(() -> new SetClocking(cpuFrequency));
	}

	public void setSampling(SamplingMethod sampling) {
		send(() -> new TrySetSampling(sampling));
	}

	public void flush() {
		send(() -> new Flush());
	}

	public void reset(byte volume) {
		send(() -> new Flush());
		send(() -> new TryReset(volume));
	}

	public void setVoiceMute(byte sidNum, byte voice, boolean mute) {
		if (version >= 3 || voice < 3) {
			send(() -> new Mute(sidNum, voice, mute));
		}
	}

	public void setVolume(byte sidNum, byte volume) {
		send(() -> new SetSidLevel(sidNum, volume));
	}

	public void setSidPosition(byte sidNum, byte position) {
		send(() -> new SetSidPosition(sidNum, position));
	}

	public byte read(byte sidNum, byte addr) {
		if (startTimeReached) {
			try {
				return tryRead(sidNum, clocksSinceLastAccess() >> fastForwardFactor, addr);
			} catch (IOException | InterruptedException e) {
				connection.close();
				throw new RuntimeException(e);
			}
		}
		return (byte) 0xff;
	}

	public void write(byte sidNum, byte addr, byte data) {
		if (startTimeReached) {
			try {
				while (tryWrite(sidNum, clocksSinceLastAccess() >> fastForwardFactor, addr, data) == BUSY)
					;
			} catch (InterruptedException | IOException e) {
				connection.close();
				throw new RuntimeException(e);
			}
		}
	}

	private Pair<ChipModel, String> sendReceiveConfig(Supplier<NetSIDPkg> cmd) {
		send(cmd);
		return new Pair<>(readResult == 1 ? ChipModel.MOS8580 : ChipModel.MOS6581, configName);
	}

	private byte sendReceive(Supplier<NetSIDPkg> cmd) {
		send(cmd);
		return readResult;
	}

	private final void send(Supplier<NetSIDPkg> cmd) {
		// transmit unsent writes
		softFlush();
		commands.add(cmd.get());
		softFlush();
	}

	void softFlush() {
		try {
			flush(false);
		} catch (IOException | InterruptedException e) {
			connection.close();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a SID read to the ring buffer, then immediately send it to
	 * NetworkSIDDevice to be queued there and executed, since a SID read is
	 * implemented to be always the last command after a series of writes
	 */
	private byte tryRead(byte sidNum, int cycles, byte addr) throws IOException, InterruptedException {
		if (!commands.isEmpty() && commands.get(0) instanceof TryWrite) {
			// Replace TryWrite by TryRead (READ terminates a series of writes)
			tryWrite = new TryRead((TryWrite) commands.remove(0), sidNum, cycles, addr);
		} else {
			// Perform a single SID read without writes (poor performance)
			tryWrite = new TryRead(sidNum, cycles, addr);
		}
		// we must send here, since a READ is always the termination of writes!
		return sendReceive(() -> tryWrite);
	}

	/**
	 * Add a SID write to the buffer, until it is full, then send it to
	 * NetworkSIDDevice to be queued there and executed
	 */
	private Response tryWrite(byte sidNum, int cycles, byte reg, byte data) throws InterruptedException, IOException {
		// flush writes after a bit of buffering. If it fails, we must cancel.
		if (maybeSendWritesToServer() == BUSY) {
			sleepDependingOnCyclesSent();
			return BUSY;
		}

		if (commands.isEmpty()) {
			// start new write buffering sequence
			tryWrite = new TryWrite();
			commands.add(tryWrite);
		}
		// add write to queue
		tryWrite.addWrite(cycles, (byte) (reg | (sidNum << 5)), data);
		// NB: if flush attempt fails, we have nevertheless queued command
		// locally and thus are allowed to return OK in any case.
		maybeSendWritesToServer();

		return OK;
	}

	private Response flush(boolean giveUpIfBusy) throws IOException, InterruptedException {
		while (!commands.isEmpty()) {
			final NetSIDPkg cmd = commands.remove(0);

			connection.send(cmd.toByteArrayWithLength());

			byte rc = readResponse();
			switch (Response.values()[rc]) {
			case VERSION:
			case READ:
			case COUNT:
				// version / read result / configuration count
				readResult = readResponse();
			case OK:
				continue;
			case BUSY:
				commands.add(0, cmd);
				if (giveUpIfBusy) {
					return BUSY;
				}
				sleepDependingOnCyclesSent();
				continue;
			case INFO:
				// chip model and SID name
				readResult = readResponse();
				configName = connection.readString();
				continue;
			default:
				connection.close();
				throw new RuntimeException("Server error: Unexpected response: " + rc);
			}
		}
		return OK;
	}

	private byte readResponse() throws IOException {
		int rc = connection.receive();
		if (rc == -1) {
			connection.close();
			throw new RuntimeException("Server closed the connection!");
		}
		return (byte) rc;
	}

	private void sleepDependingOnCyclesSent() throws InterruptedException {
		if (tryWrite.getCyclesSendToServer() > BUFFER_NEAR_FULL) {
			Thread.sleep(tryWrite.getCyclesSendToServer() / CYCLES_TO_MILLIS - 3);
		}
	}

	/**
	 * Flush writes after a bit of buffering
	 */
	private Response maybeSendWritesToServer() throws IOException, InterruptedException {
		if (commands.size() >= MAX_BUFFER_SIZE || tryWrite.getCyclesSendToServer() >= MAX_WRITE_CYCLES) {
			if (flush(true) == BUSY) {
				return BUSY;
			}
		}
		return OK;
	}

	private int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return diff;
	}

	private long eventuallyDelay(byte sidNum) {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY) {
			lastSIDWriteTime += REGULAR_DELAY;
			send(() -> new TryDelay(sidNum, REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}

	public void start() {
		startTimeReached = true;
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	public void fastForward() {
		fastForwardFactor++;
	}

	public void normalSpeed() {
		fastForwardFactor = 0;
	}

	public boolean isFastForward() {
		return fastForwardFactor != 0;
	}

	public int getFastForwardBitMask() {
		return (1 << fastForwardFactor) - 1;
	}

}
