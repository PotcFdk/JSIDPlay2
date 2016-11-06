package netsiddev_builder;

import static netsiddev_builder.NetSIDResponse.BUSY;
import static netsiddev_builder.NetSIDResponse.OK;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import libsidplay.components.pla.PLA;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.NetSIDPkg;
import netsiddev_builder.commands.SetSIDClocking;
import netsiddev_builder.commands.SetSIDCount;
import netsiddev_builder.commands.SetSIDLevel;
import netsiddev_builder.commands.SetSIDModel;
import netsiddev_builder.commands.SetSIDPosition;
import netsiddev_builder.commands.TryDelay;
import netsiddev_builder.commands.TryReset;
import netsiddev_builder.commands.TryWrite;

public class NetSIDConnection {

	private static final int PORT = 6581;
	private static final String HOSTNAME = "127.0.0.1";
	private static final int MAX_WRITE_CYCLES = 4096; /* c64 cycles */
	private static final int CMD_BUFFER_SIZE = 4096;

	private static Socket connectedSocket;
	private List<NetSIDPkg> commands = new ArrayList<>();
	private TryWrite tryWrite;
	private byte result[] = new byte[2];

	public NetSIDConnection(IConfig config, SidTune tune) {
		try {
			connectedSocket = new Socket(HOSTNAME, PORT);

			commands.add(new SetSIDCount((byte) PLA.MAX_SIDS));
			for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
				commands.add(new SetSIDModel((byte) sidNum, (byte) sidNum));
				commands.add(new SetSIDLevel((byte) sidNum, (byte) 0));
				commands.add(new SetSIDPosition((byte) sidNum, (byte) 0));
			}
			flush(false, null);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void flush(byte sidNum) {
		commands.add(new Flush(sidNum));
		try {
			flush(false, null);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void reset(byte sidNum, byte volume) {
		commands.add(new TryReset(sidNum, volume));
		try {
			flush(false, null);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void addWrite(byte sidNum, int cycles, byte addr, byte data) {
		try {
			while (tryWrite(sidNum, cycles, (byte) addr, data) == BUSY) {
				// Try_Write sleeps for us
			}
		} catch (InterruptedException | IOException e) {
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
		tryWrite.addWrite(cycles, reg, data);
		/*
		 * NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case.
		 */
		maybe_send_writes_to_server();

		return OK;
	}

	private NetSIDResponse flush(boolean give_up_if_busy, byte[] readResult) throws IOException, InterruptedException {
		while (!commands.isEmpty()) {
			final NetSIDPkg cmd = commands.get(0);

			connectedSocket.getOutputStream().write(cmd.toByteArrayWithLength());
			connectedSocket.getInputStream().read(result);

			switch (NetSIDResponse.values()[result[0]]) {
			case READ:
				readResult[0] = result[1];
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
		if (tryWrite.getCyclesSentToServer() > 3072) {
			Thread.sleep(Math.max(1, tryWrite.getCyclesSentToServer() / 1000 - 3));
		}
	}

	private NetSIDResponse maybe_send_writes_to_server() throws IOException, InterruptedException {
		/* flush writes after a bit of buffering */
		if (commands.size() == CMD_BUFFER_SIZE
				|| (tryWrite != null && tryWrite.getCyclesSentToServer() > MAX_WRITE_CYCLES)) {
			if (flush(true, null) == BUSY) {
				return BUSY;
			}
		}
		return OK;
	}

	public void delay(byte sidNum, byte cycles) {
		try {
			/* deal with unsubmitted writes */
			flush(false, null);

			commands.add(new TryDelay(sidNum, cycles));
			flush(false, null);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void setClockFrequency(byte sidNum, double cpuFrequency) {
		try {
			commands.add(new SetSIDClocking(sidNum, cpuFrequency));
			flush(false, null);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			connectedSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
