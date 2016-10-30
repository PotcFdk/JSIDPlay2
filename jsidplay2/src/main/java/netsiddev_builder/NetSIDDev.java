package netsiddev_builder;

import static netsiddev_builder.NetSIDCommand.CMD_FLUSH;
import static netsiddev_builder.NetSIDCommand.CMD_TRY_DELAY;
import static netsiddev_builder.NetSIDCommand.CMD_TRY_RESET;
import static netsiddev_builder.NetSIDCommand.CMD_TRY_SET_SID_COUNT;
import static netsiddev_builder.NetSIDCommand.CMD_TRY_WRITE;
import static netsiddev_builder.NetSIDCommand.TRY_SET_SID_MODEL;
import static netsiddev_builder.NetSIDResponse.BUSY;
import static netsiddev_builder.NetSIDResponse.OK;
import static netsiddev_builder.NetSIDResponse.READ;

import java.io.IOException;
import java.net.Socket;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {
	private static final int DELAY_CYCLES = 250;
	private static final int MAX_WRITE_CYCLES = 4096; /* c64 cycles */

	private static final int WAIT_BETWEEN_ATTEMPTS = 2; /* ms */

	private static final int CMD_BUFFER_SIZE = 4096;

	private int sidCnt;
	private Socket connectedSocket;
	private int sidNum;

	// writes buffered at client
	byte cmd_buffer[] = new byte[CMD_BUFFER_SIZE];
	// index at cmd_buffer.
	int cmd_index;
	// cycles queued in command.
	int cmd_buffer_cycles;

	private final Event event = new Event("NetSIDDev Delay") {
		@Override
		public void event() {
			int cycles = delay();
			if (cycles > 0)
				delay(sidNum, cycles);
			context.schedule(event, DELAY_CYCLES, Event.Phase.PHI2);
		}
	};

	private int delay() {
		int cycles = clocksSinceLastAccess();
		while (cycles > 0xFFFF) {
			delay(sidNum, 0xFFFF);
			cycles -= 0xFFFF;
		}
		return cycles;
	}

	private void delay(int sidNum, int cycles) {
		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			try {
				flush_cmd_buffer(false, null);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			cmd_buffer_cycles = 0;
		}

		cmd_buffer[cmd_index++] = CMD_TRY_DELAY.cmd;
		cmd_buffer[cmd_index++] = (byte) sidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = (byte) ((cycles & 0xff00) >> 8);
		cmd_buffer[cmd_index++] = (byte) (cycles & 0xff);
		try {
			flush_cmd_buffer(false, null);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public NetSIDDev(EventScheduler context, final int sidNum, final ChipModel model) {
		super(context);
		this.sidNum = sidNum;
	}

	public void lock() {
		if (connectedSocket == null) {
			try {
				connectedSocket = new Socket("127.0.0.1", 6581);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		reset((byte) 0x0);
		context.schedule(event, 0, Event.Phase.PHI2);
		sidCnt++;
	}

	public void unlock() {
		if (connectedSocket != null && sidCnt == 1) {
			try {
				connectedSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			connectedSocket = null;
		}
		sidCnt--;
		context.cancel(event);
	}

	@Override
	public void reset(byte volume) {
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_TRY_SET_SID_COUNT.cmd;
		cmd_buffer[cmd_index++] = (byte) 1; /* SID count */
		cmd_index += 2;
		try {
			flush_cmd_buffer(false, null);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = TRY_SET_SID_MODEL.cmd;
		cmd_buffer[cmd_index++] = (byte) sidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = 0 /* SID model */;
		try {
			flush_cmd_buffer(false, null);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_FLUSH.cmd;
		cmd_buffer[cmd_index++] = (byte) sidNum; /* SID number */
		cmd_index += 2;
		try {
			flush_cmd_buffer(false, null);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		cmd_buffer[cmd_index++] = CMD_TRY_RESET.cmd;
		cmd_buffer[cmd_index++] = (byte) sidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = 0;
		try {
			flush_cmd_buffer(false, null);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte read(int addr) {
		return (byte) 0xff;
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		int cycles = delay();
		try {
			while (tryWrite((byte) sidNum, cycles, (byte) addr, data) == BUSY) {
				// Try_Write sleeps for us
			}
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

	// Add a SID write to the ring buffer, until it is full,
	// then send it to JSIDPlay2 to be queued there and executed
	NetSIDResponse tryWrite(byte SidNum, int Cycles, byte SID_reg, byte Data) throws InterruptedException, IOException {
		/*
		 * flush writes after a bit of buffering. If no flush, then returns OK
		 * and we queue more. If flush attempt fails, we must cancel.
		 */
		if (maybe_send_writes_to_server() == BUSY) {
			/*
			 * Sigh. Acid64 is daft. Why doesn't it sleep if it gets a BUSY code
			 * but immediately retries? That eliminates the whole point I
			 * thought Try_Write had. With a logic like this HardSID_Write works
			 * just as good as this. Damnit.
			 */
			Thread.sleep(WAIT_BETWEEN_ATTEMPTS);
			return BUSY;
		}

		if (cmd_index == 0) {
			/* start new write buffering sequence */
			cmd_buffer[cmd_index++] = CMD_TRY_WRITE.cmd;
			cmd_buffer[cmd_index++] = SidNum;
			cmd_index += 2;
		}

		/* add write to queue */
		cmd_buffer[cmd_index++] = (byte) ((Cycles & 0xff00) >> 8);
		cmd_buffer[cmd_index++] = (byte) (Cycles & 0xff);
		cmd_buffer[cmd_index++] = SID_reg;
		cmd_buffer[cmd_index++] = Data;
		cmd_buffer_cycles += Cycles;
		/*
		 * NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case.
		 */
		maybe_send_writes_to_server();

		return OK;
	}

	NetSIDResponse flush_cmd_buffer(boolean give_up_if_busy, byte[] readResult)
			throws IOException, InterruptedException {
		while (true) {
			byte result[] = new byte[2];
			/*
			 * Fill in packet data length so that server knows when entire
			 * packet has been read.
			 */
			int data_length = cmd_index - 4;
			cmd_buffer[2] = (byte) ((data_length >> 8) & 0xff);
			cmd_buffer[3] = (byte) (data_length & 0xff);

			connectedSocket.getOutputStream().write(cmd_buffer, 0, cmd_index);

			connectedSocket.getInputStream().read(result);

			int rc = result[0];

			/* server accepted. Reset variables. */
			if (rc == OK.resp) {
				cmd_index = 0;
				return OK;
			}

			if (rc == BUSY.resp) {
				if (give_up_if_busy) {
					return BUSY;
				}
				Thread.sleep(WAIT_BETWEEN_ATTEMPTS);
				continue;
			}

			/*
			 * the only caller that uses TRY_READ passes "false" on
			 * give_up_if_busy, so this is the only way this can end, barring
			 * errors.
			 */
			if (rc == READ.resp) {
				cmd_index = 0;
				readResult[0] = result[1];
				return OK;
			}
			throw new RuntimeException("Server error");
		}
	}

	NetSIDResponse maybe_send_writes_to_server() throws IOException, InterruptedException {
		/* flush writes after a bit of buffering */
		if (cmd_index == cmd_buffer.length || cmd_buffer_cycles > MAX_WRITE_CYCLES) {
			if (flush_cmd_buffer(true, null) == BUSY) {
				return BUSY;
			}
			cmd_buffer_cycles = 0;
		}
		return OK;
	}

	@Override
	public void clock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setChipModel(ChipModel model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
		// TODO Auto-generated method stub

	}

	@Override
	public void input(int input) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getInputDigiBoost() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setVoiceMute(int num, boolean mute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		// TODO Auto-generated method stub

	}

}
