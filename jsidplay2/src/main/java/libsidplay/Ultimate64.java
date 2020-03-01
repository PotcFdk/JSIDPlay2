package libsidplay;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Locale;

import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

/**
 * Makes use of the Ultimate64 (FPGA-C64) debug interface to use JSIDPlay2 and
 * Ultimate64 simultaneously.
 * 
 * https://github.com/GideonZ/1541ultimate/blob/master/software/network/socket_dma.cc
 * 
 * https://github.com/GideonZ/1541ultimate/blob/master/python/sock.py
 * 
 * https://1541u-documentation.readthedocs.io/en/latest/data_streams.html
 * 
 * @author ken
 *
 */
public interface Ultimate64 {
	enum SocketCommand {
		// "Ok ok, use them then..."
		SOCKET_CMD_DMA(0xFF01), SOCKET_CMD_DMARUN(0xFF02), SOCKET_CMD_KEYB(0xFF03), SOCKET_CMD_RESET(0xFF04),
		SOCKET_CMD_WAIT(0xFF05), SOCKET_CMD_DMAWRITE(0xFF06), SOCKET_CMD_REUWRITE(0xFF07),
		SOCKET_CMD_KERNALWRITE(0xFF08), SOCKET_CMD_DMAJUMP(0xFF09), INSERT_DISK(0xFF0A), SOCKET_CMD_RUN_IMG(0xFF0B),
		// Undocumented, shall only be used by developers.
		SOCKET_CMD_LOADSIDCRT(0xFF71), SOCKET_CMD_LOADBOOTCRT(0xFF72), SOCKET_CMD_READMEM(0xFF74),
		SOCKET_CMD_READFLASH(0xFF75), SOCKET_CMD_DEBUG_REG(0xFF76);

		int value;

		private SocketCommand(int on) {
			this.value = on;
		}
	}

	enum SocketStreamingCommand {
		// Only available on U64
		SOCKET_CMD_VICSTREAM_ON(0xFF20), SOCKET_CMD_AUDIOSTREAM_ON(0xFF21), SOCKET_CMD_VICSTREAM_OFF(0xFF30),
		SOCKET_CMD_AUDIOSTREAM_OFF(0xFF31);

		int value;

		private SocketStreamingCommand(int on) {
			this.value = on;
		}
	}

	/**
	 * Ultimate64 socket connection timeout.
	 */
	int SOCKET_CONNECT_TIMEOUT = 5000;

	/**
	 * Maximum length for a user typed-in command.
	 */
	int MAX_COMMAND_LEN = 16;

	/**
	 * Send RAM to Ultimate64 C64-RAM and type Run.<BR>
	 * 
	 * <B>Note:</B> whole RAM is currently transfered, no matter how long the
	 * program is.
	 * 
	 * @param config configuration
	 * @param tune   tune to play
	 * @param c64Ram C64 emulator RAM
	 * @throws InterruptedException
	 */
	default void sendRamAndRun(IConfig config, SidTune tune, byte[] c64Ram) throws InterruptedException {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		int syncDelay = config.getEmulationSection().getUltimate64SyncDelay();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			int ramStart = 0x0800;
			int ramEnd = 0x10000;
			byte[] ram = new byte[ramEnd - ramStart + 6];
			ram[0] = (byte) (SocketCommand.SOCKET_CMD_DMARUN.value & 0xff);
			ram[1] = (byte) ((SocketCommand.SOCKET_CMD_DMARUN.value >> 8) & 0xff);
			ram[2] = (byte) (ram.length & 0xff);
			ram[3] = (byte) ((ram.length >> 8) & 0xff);
			ram[4] = (byte) (ramStart & 0xff);
			ram[5] = (byte) ((ramStart >> 8) & 0xff);
			System.arraycopy(c64Ram, ramStart, ram, 6, ramEnd - ramStart);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM and RUN: " + e.getMessage());
		}
		Thread.sleep(syncDelay);
	}

	/**
	 * Send RAM to Ultimate64 C64-RAM and start machine code.<BR>
	 * 
	 * <B>Note:</B> whole RAM is currently transfered, no matter how long the
	 * program is.
	 * 
	 * @param config    configuration
	 * @param tune      tune to play
	 * @param c64Ram    C64 emulator RAM
	 * @param startAddr start address of machine code to execute
	 * @throws InterruptedException
	 */
	default void sendRamAndSys(IConfig config, SidTune tune, byte[] c64Ram, int startAddr) throws InterruptedException {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		int syncDelay = config.getEmulationSection().getUltimate64SyncDelay();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			int ramStart = 0x0400;
			int ramEnd = 0x10000;
			byte[] ram = new byte[ramEnd - ramStart + 8];
			ram[0] = (byte) (SocketCommand.SOCKET_CMD_DMAJUMP.value & 0xff);
			ram[1] = (byte) ((SocketCommand.SOCKET_CMD_DMAJUMP.value >> 8) & 0xff);
			ram[2] = (byte) (ram.length & 0xff);
			ram[3] = (byte) ((ram.length >> 8) & 0xff);
			ram[4] = (byte) (startAddr & 0xff);
			ram[5] = (byte) ((startAddr >> 8) & 0xff);
			ram[6] = (byte) (ramStart & 0xff);
			ram[7] = (byte) ((ramStart >> 8) & 0xff);
			System.arraycopy(c64Ram, ramStart, ram, 8, ramEnd - ramStart);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM and SYS: " + e.getMessage());
		}
		Thread.sleep(syncDelay);
	}

	/**
	 * Send Reset to Ultimate64.<BR>
	 * 
	 * @param config configuration
	 * @param tune   tune to play
	 */
	default void sendReset(IConfig config, SidTune tune) {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[4];
			ram[0] = (byte) (SocketCommand.SOCKET_CMD_RESET.value & 0xff);
			ram[1] = (byte) ((SocketCommand.SOCKET_CMD_RESET.value >> 8) & 0xff);
			ram[2] = 0;
			ram[3] = 0;
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RESET: " + e.getMessage());
		}
	}

	/**
	 * Send a keyboard command, as if a user pressed these keys.<BR>
	 * 
	 * <B>Note:</B> command length is limited to max. 16 characters. You can
	 * simulate a return press by sending carriage return (e.g.
	 * "LOAD\"*\",8,1\rRUN\r"). Sending special characters or pressing additional
	 * keys like shift is not supported.
	 * 
	 * @param config  configuration
	 * @param command command to send
	 */
	default void sendCommand(IConfig config, String command) {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[length + 4];
			ram[0] = (byte) (SocketCommand.SOCKET_CMD_KEYB.value & 0xff);
			ram[1] = (byte) ((SocketCommand.SOCKET_CMD_KEYB.value >> 8) & 0xff);
			ram[2] = (byte) (length & 0xff);
			ram[3] = (byte) ((length >> 8) & 0xff);
			System.arraycopy(command.toUpperCase(Locale.US).getBytes(US_ASCII), 0, ram, 4, length);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send COMMAND: " + e.getMessage());
		}
	}

	/**
	 * Wait on the Ultimate64 server side
	 * 
	 * @param config configuration
	 * @param delay  delay to wait (600 ~ 3 seconds)
	 */
	default void sendWait(IConfig config, int delay) {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[4/*6*/];
			ram[0] = (byte) (SocketCommand.SOCKET_CMD_WAIT.value & 0xff);
			ram[1] = (byte) ((SocketCommand.SOCKET_CMD_WAIT.value >> 8) & 0xff);
			// XXX Should be length as delay (delayLow delayHigh?) TEST ME!
			ram[2] = (byte) (delay & 0xff);
			ram[3] = (byte) ((delay >> 8) & 0xff);
//			ram[4] = (byte) (delay & 0xff);
//			ram[5] = (byte) ((delay >> 8) & 0xff);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send WAIT: " + e.getMessage());
		}
	}

	/**
	 * Insert D64 image into Ultimate64 floppy disk drive.
	 * 
	 * @param config configuration
	 * @param file   d64 file
	 * @throws IOException I/O error
	 */
	default void sendInsertDisk(IConfig config, final File file) throws IOException {
		byte diskContents[] = Files.readAllBytes(file.toPath());
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[diskContents.length + 5];
			ram[0] = (byte) (SocketCommand.INSERT_DISK.value & 0xff);
			ram[1] = (byte) ((SocketCommand.INSERT_DISK.value >> 8) & 0xff);
			ram[2] = (byte) (diskContents.length & 0xff);
			ram[3] = (byte) ((diskContents.length >> 8) & 0xff);
			ram[4] = (byte) ((diskContents.length >> 16) & 0xff);
			System.arraycopy(diskContents, 0, ram, 5, diskContents.length);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot insert disk: " + e.getMessage());
		}
	}

	/**
	 * Start streaming.
	 * 
	 * <pre>
	 * 192.168.0.119:11000 unicast address on the local network, port number 11000
	 * myserver.com unicast address, using DNS and default port number
	 * myserver.com:4567 unicast address, using DNS and specific port number
	 * 239.0.1.64 multicast address, using default port number
	 * 239.0.2.77:64738 multicast address with port number specified
	 * </pre>
	 * 
	 * @param emulationSection emulation configuration
	 * @param command          streaming VIC/SID
	 * @param target           network target to receive the stream
	 * @param duration         duration in ticks (one tick is 5ms, 0=forever)
	 */
	default void startStreaming(IEmulationSection emulationSection, SocketStreamingCommand command, String target,
			int duration) {
		String hostname = emulationSection.getUltimate64Host();
		int port = emulationSection.getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[target.length() + 6];
			ram[0] = (byte) (command.value & 0xff);
			ram[1] = (byte) ((command.value >> 8) & 0xff);
			ram[2] = (byte) ((target.length() + 2) & 0xff);
			ram[3] = (byte) (((target.length() + 2) >> 8) & 0xff);
			ram[4] = 0;
			ram[5] = (byte) (duration & 0xff);
			System.arraycopy(target.getBytes(US_ASCII), 0, ram, 6, target.length());
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot start streaming: " + e.getMessage());
		}
	}

	/**
	 * Stop streaming.
	 * 
	 * @param emulationSection emulation configuration
	 * @param command          streaming VIC/SID
	 */
	default void stopStreaming(IEmulationSection emulationSection, SocketStreamingCommand command) {
		String hostname = emulationSection.getUltimate64Host();
		int port = emulationSection.getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[4];
			ram[0] = (byte) (command.value & 0xff);
			ram[1] = (byte) ((command.value >> 8) & 0xff);
			ram[2] = 0;
			ram[3] = 0;
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot stop streaming: " + e.getMessage());
		}
	}
}
