package libsidplay;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Locale;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;

public interface Ultimate64 {

	/**
	 * Ultimate64 socket connection timeout.
	 */
	int SOCKET_CONNECT_TIMEOUT = 5000;

	/**
	 * Maximum length for a user typed-in command.
	 */
	int MAX_COMMAND_LEN = 16;

	Charset US_ASCII = Charset.forName("US-ASCII");

	default void sendInsertDisk(IConfig config, final File file) throws IOException {
		RandomAccessFile fd = new RandomAccessFile(file, file.canWrite() ? "rw" : "r");
		// Try to detect image type
		final byte diskContents[] = new byte[(int) file.length()];
		fd.readFully(diskContents, 0, diskContents.length);
		fd.close();
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[diskContents.length + 5];
			ram[0] = (byte) 0x0a;
			ram[1] = (byte) 0xff;
			ram[2] = (byte) (diskContents.length & 0xff);
			ram[3] = (byte) ((diskContents.length >> 8) & 0xff);
			ram[4] = (byte) ((diskContents.length >> 16) & 0xff);
			System.arraycopy(diskContents, 0, ram, 5, diskContents.length);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM: " + e.getMessage());
		}
	}

	default void sendRamAndSys(IConfig config, SidTune tune, byte[] c64Ram, int startAddr) throws InterruptedException {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		int syncDelay = config.getEmulationSection().getUltimate64SyncDelay();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			int ramStart = 0x0400;
			int ramEnd = 0x10000;
			byte[] ram = new byte[ramEnd - ramStart + 8];
			ram[0] = (byte) 0x09;
			ram[1] = (byte) 0xff;
			ram[2] = (byte) (ram.length & 0xff);
			ram[3] = (byte) ((ram.length >> 8) & 0xff);
			ram[4] = (byte) (startAddr & 0xff);
			ram[5] = (byte) ((startAddr >> 8) & 0xff);
			ram[6] = (byte) (ramStart & 0xff);
			ram[7] = (byte) ((ramStart >> 8) & 0xff);
			System.arraycopy(c64Ram, ramStart, ram, 8, ramEnd - ramStart);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM: " + e.getMessage());
		}
		Thread.sleep(SidTune.getInitDelay(tune) / 1000);
		Thread.sleep(syncDelay);
	}

	default void sendRamAndRun(IConfig config, SidTune tune, byte[] c64Ram) throws InterruptedException {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		int syncDelay = config.getEmulationSection().getUltimate64SyncDelay();
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			int ramStart = 0x0800;
			int ramEnd = 0x10000;
			byte[] ram = new byte[ramEnd - ramStart + 6];
			ram[0] = (byte) 0x02;
			ram[1] = (byte) 0xff;
			ram[2] = (byte) (ram.length & 0xff);
			ram[3] = (byte) ((ram.length >> 8) & 0xff);
			ram[4] = (byte) (ramStart & 0xff);
			ram[5] = (byte) ((ramStart >> 8) & 0xff);
			System.arraycopy(c64Ram, ramStart, ram, 6, ramEnd - ramStart);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM: " + e.getMessage());
		}
		Thread.sleep(SidTune.getInitDelay(tune) / 1000);
		Thread.sleep(syncDelay);
	}

	default void sendCommand(IConfig config, String command) throws InterruptedException {
		String hostname = config.getEmulationSection().getUltimate64Host();
		int port = config.getEmulationSection().getUltimate64Port();
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		try (Socket connectedSocket = new Socket()) {
			connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
			byte[] ram = new byte[length + 4];
			ram[0] = (byte) 0x03;
			ram[1] = (byte) 0xff;
			ram[2] = (byte) (length);
			ram[3] = (byte) (0);
			System.arraycopy(command.toUpperCase(Locale.US).getBytes(US_ASCII), 0, ram, 4, length);
			connectedSocket.getOutputStream().write(ram);
		} catch (IOException e) {
			System.err.println("Ultimate64: cannot send RAM: " + e.getMessage());
		}
	}
}
