package netsiddev_builder;

import static netsiddev_builder.NetSIDResponse.BUSY;
import static netsiddev_builder.NetSIDResponse.OK;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.pla.PLA;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.GetConfigCount;
import netsiddev_builder.commands.GetConfigInfo;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.NetSIDPkg;
import netsiddev_builder.commands.SetSIDClocking;
import netsiddev_builder.commands.SetSIDCount;
import netsiddev_builder.commands.SetSIDLevel;
import netsiddev_builder.commands.SetSIDModel;
import netsiddev_builder.commands.SetSIDPosition;
import netsiddev_builder.commands.SetSIDSampling;
import netsiddev_builder.commands.TryDelay;
import netsiddev_builder.commands.TryRead;
import netsiddev_builder.commands.TryReset;
import netsiddev_builder.commands.TryWrite;

public class NetSIDConnection {

	private static final int PORT = 6581;
	private static final String HOSTNAME = "127.0.0.1";

	private static final int MAX_WRITE_CYCLES = 4096;
	private static final int CMD_BUFFER_SIZE = 4096;
	private static final int REGULAR_DELAY = 0xFFFF;

	private EventScheduler context;
	private static Socket connectedSocket;
	private List<NetSIDPkg> commands = new ArrayList<>();
	private TryWrite tryWrite;
	private byte readResult, configInfo[] = new byte[255];
	private long lastSIDWriteTime;
	private Map<String, Byte> filterNameToSIDModel = new HashMap<>();

	public NetSIDConnection(EventScheduler context, IConfig config, SidTune tune) {
		this.context = context;
		try {
			if (connectedSocket == null || !connectedSocket.isConnected()) {
				connectedSocket = new Socket(HOSTNAME, PORT);
			}

			// Check available SIDs
			for (byte i = 0; i < (int) getConfigCount(); i++) {
				byte[] chipModel = new byte[1];
				String name = getConfigInfo(i, chipModel);
				ChipModel model = chipModel[0] == 1 ? ChipModel.MOS8580 : ChipModel.MOS6581;
				System.out.println(name + " (" + model + ")");
				filterNameToSIDModel.put("Filter" + name, i);
			}
			// Initialize SIDs on server side
			commands.add(new SetSIDCount((byte) PLA.MAX_SIDS));
			for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
				commands.add(new SetSIDModel((byte) sidNum, (byte) sidNum));
			}
			// Set global settings
			for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
				commands.add(
						new SetSIDSampling((byte) sidNum, (byte) config.getAudioSection().getSampling().ordinal()));
				commands.add(new SetSIDLevel((byte) sidNum, (byte) 0));
				commands.add(new SetSIDPosition((byte) sidNum, (byte) 0));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setFilter(byte sidNum, String filterName) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			Byte model = filterNameToSIDModel.get(filterName);
			if (model == null) {
				model = sidNum;
				System.err.println("Undefined Filter: " + filterName + ", will use instead: " + sidNum);
			}
			commands.add(new SetSIDModel((byte) sidNum, model));
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void setClockFrequency(byte sidNum, double cpuFrequency) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new SetSIDClocking(sidNum, cpuFrequency));
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void reset(byte sidNum, byte volume) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new Flush(sidNum));
			commands.add(new TryReset(sidNum, volume));
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public byte read(byte sidNum, byte addr) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new TryRead(sidNum, clocksSinceLastAccess(), addr));
			flush(false);
			return readResult;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void write(byte sidNum, byte addr, byte data) {
		try {
			while (tryWrite(sidNum, clocksSinceLastAccess(), addr, data) == BUSY) {
				// Try_Write sleeps for us
			}
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void mute(byte sidNum, byte voice, boolean mute) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new Mute(sidNum, voice, mute));
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private byte getConfigCount() {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new GetConfigCount());
			flush(false);
			return readResult;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private String getConfigInfo(byte sidNum, byte[] chipModel) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new GetConfigInfo(sidNum));
			flush(false);
			chipModel[0] = readResult;
			int i = 0;
			for (; configInfo[i] != 0 && i < configInfo.length; i++) {
			}
			return new String(configInfo, 0, i, "ISO-8859-1");
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	// Add a SID write to the ring buffer, until it is full,
	// then send it to JSIDPlay2 to be queued there and executed
	private NetSIDResponse tryWrite(byte sidNum, int cycles, byte reg, byte data)
			throws InterruptedException, IOException {
		/*
		 * flush writes after a bit of buffering. If no flush, then returns OK
		 * and we queue more. If flush attempt fails, we must cancel.
		 */
		if (maybe_send_writes_to_server() == BUSY) {
			sleepDependingOnCyclesSent();
			return BUSY;
		}

		if (commands.isEmpty()) {
			/* start new write buffering sequence */
			tryWrite = new TryWrite(sidNum);
			commands.add(tryWrite);
		}
		/* add write to queue */
		tryWrite.addWrite(cycles, (byte) (reg | (sidNum << 5)), data);
		/*
		 * NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case.
		 */
		maybe_send_writes_to_server();

		return OK;
	}

	private NetSIDResponse flush(boolean give_up_if_busy) throws IOException, InterruptedException {
		while (!commands.isEmpty()) {
			final NetSIDPkg cmd = commands.get(0);

			connectedSocket.getOutputStream().write(cmd.toByteArrayWithLength());
			int rc = connectedSocket.getInputStream().read();
			if (rc == -1) {
				connectedSocket.close();
				throw new RuntimeException("Server closed the connection!");
			}
			switch (NetSIDResponse.values()[rc]) {
			case INFO:
				// chip model
				readResult = (byte) connectedSocket.getInputStream().read();
				// 0 terminated name
				for (int i = 0; i < 255; i++) {
					connectedSocket.getInputStream().read(configInfo, i, 1);
					if (configInfo[i] == 0)
						break;
				}
				commands.remove(0);
				continue;
			case READ:
			case COUNT:
				// read result or configuration count
				readResult = (byte) connectedSocket.getInputStream().read();
			case OK:
				commands.remove(0);
				continue;
			case BUSY:
				if (give_up_if_busy) {
					return BUSY;
				}
				sleepDependingOnCyclesSent();
				continue;

			default:
				throw new RuntimeException("Server error: Unexpected response!");
			}
		}
		return OK;
	}

	private void sleepDependingOnCyclesSent() throws InterruptedException {
		if (tryWrite.getCyclesSentToServer() > (MAX_WRITE_CYCLES * 3 >> 2)) {
			Thread.sleep(Math.max(1, tryWrite.getCyclesSentToServer() / 1000 - 3));
		}
	}

	private NetSIDResponse maybe_send_writes_to_server() throws IOException, InterruptedException {
		/* flush writes after a bit of buffering */
		if (commands.size() == CMD_BUFFER_SIZE
				|| (tryWrite != null && tryWrite.getCyclesSentToServer() > MAX_WRITE_CYCLES)) {
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

	long eventuallyDelay(byte sidNum) {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY << 1) {
			lastSIDWriteTime += REGULAR_DELAY;
			delay(sidNum, REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}

	private void delay(byte sidNum, int cycles) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			commands.add(new TryDelay(sidNum, cycles));
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
