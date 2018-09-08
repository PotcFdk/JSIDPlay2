package builder.netsiddev;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.netsiddev.Response.BUSY;
import static server.netsiddev.Response.OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import builder.netsiddev.commands.Flush;
import builder.netsiddev.commands.GetConfigCount;
import builder.netsiddev.commands.GetConfigInfo;
import builder.netsiddev.commands.GetVersion;
import builder.netsiddev.commands.NetSIDPkg;
import builder.netsiddev.commands.TryDelay;
import builder.netsiddev.commands.TryRead;
import builder.netsiddev.commands.TryReset;
import builder.netsiddev.commands.TrySetSidCount;
import builder.netsiddev.commands.TrySetSidModel;
import builder.netsiddev.commands.TryWrite;
import javafx.util.Pair;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.config.IEmulationSection;
import server.netsiddev.Response;

public class NetSIDClient {
	private static final int CYCLES_TO_MILLIS = 1000;
	private static final int MAX_WRITE_CYCLES = 4096;
	private static final int MAX_BUFFER_SIZE = 4096;
	private static final int BUFFER_NEAR_FULL = MAX_WRITE_CYCLES * 3 >> 2;
	private static final int REGULAR_DELAY = MAX_WRITE_CYCLES >> 2;

	private NetSIDDevConnection connection = NetSIDDevConnection.getInstance();

	private static byte version;
	private final EventScheduler context;
	private final List<NetSIDPkg> commands = new ArrayList<>(MAX_BUFFER_SIZE);
	private TryWrite tryWrite = new TryWrite();
	private byte readResult;
	private String configName;
	private long lastSIDWriteTime;
	private int fastForwardFactor;

	private final Event event = new Event("JSIDDevice Delay") {

		@Override
		public void event() {
			// Pure delay is added to the server side queue for all SIDs in use.
			// Note: Hard-wired SID chip number zero; sid_detection.prg seems
			// to correctly detect SID chip type even using a fake stereo SID
			// with a different model!?
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
		boolean wasNotConnectedYet = connection.isDisconnected();
		try {
			connection.open(emulationSection.getNetSIDDevHost(), emulationSection.getNetSIDDevPort());
		} catch (IOException e) {
			connection.close();
			System.err.printf("Creating connection for %s:%d failed!\n", emulationSection.getNetSIDDevHost(),
					emulationSection.getNetSIDDevPort());
			throw new RuntimeException(e.getMessage());
		}
		// Get version and all available SID models once per connection
		if (TrySetSidModel.getFilterToSidModel().isEmpty() || wasNotConnectedYet) {
			version = sendReceive(new GetVersion());
			TrySetSidModel.getFilterToSidModel().clear();
			for (byte config = 0; config < sendReceive(new GetConfigCount()); config++) {
				Pair<ChipModel, String> filter = sendReceiveConfig(new GetConfigInfo(config));
				TrySetSidModel.getFilterToSidModel().put(filter, config);
			}
			addSetSidModels();
			softFlush();
		}
	}

	public byte getVersion() {
		return version;
	}

	/**
	 * Add setting all SidModels to the first available configuration to the
	 * command queue to initialize server side properly.
	 */
	private void addSetSidModels() {
		commands.add(new TrySetSidCount((byte) MAX_SIDS));
		for (byte sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
			commands.add(new TrySetSidModel(sidNum, (byte) 0));
		}
	}

	/**
	 * Initialize: Drop unprocessed writes and add flush and reset to the
	 * command queue
	 * 
	 * @param volume
	 *            volume for reset
	 */
	public void init(byte volume) {
		clocksSinceLastAccess();
		commands.clear();
		add(new Flush());
		add(new TryReset(volume));
	}

	public byte read(byte sidNum, byte addr) {
		try {
			return tryRead(sidNum, clocksSinceLastAccess() >> fastForwardFactor, addr);
		} catch (IOException | InterruptedException e) {
			connection.close();
			throw new RuntimeException(e);
		}
	}

	public void write(byte sidNum, byte addr, byte data) {
		try {
			int clocksSinceLastAccess = clocksSinceLastAccess();
			while (tryWrite(sidNum, clocksSinceLastAccess >> fastForwardFactor, addr, data) == BUSY)
				;
		} catch (InterruptedException | IOException e) {
			connection.close();
			throw new RuntimeException(e);
		}
	}

	private Pair<ChipModel, String> sendReceiveConfig(NetSIDPkg cmd) {
		addAndSend(cmd);
		return new Pair<>(readResult == 1 ? ChipModel.MOS8580 : ChipModel.MOS6581, configName);
	}

	private byte sendReceive(NetSIDPkg cmd) {
		addAndSend(cmd);
		return readResult;
	}

	final void addAndSend(NetSIDPkg cmd) {
		// transmit unsent writes
		softFlush();
		add(cmd);
		softFlush();
	}

	final boolean add(NetSIDPkg cmd) {
		return commands.add(cmd);
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
		return sendReceive(tryWrite);
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
		tryWrite.addWrite(cycles, (byte) ((sidNum << 5) | reg), data);
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
				configName = connection.receiveString();
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
			addAndSend(new TryDelay(sidNum, REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}

	public void start() {
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
