package builder.netsiddev;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class NetSIDDevConnection {
	/**
	 * Timeout to establish a connection to a NetworkSIDDevice im ms.
	 */
	private static final int SOCKET_CONNECT_TIMEOUT = 5000;
	/**
	 * Maximum string length read from server.
	 */
	private static final int MAX_STRING_LENGTH = 255;
	/**
	 * Expected string encoding read from server.
	 */
	private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	private boolean invalidated;
	private Socket connectedSocket;
	private byte[] stringBytes = new byte[MAX_STRING_LENGTH];

	/**
	 * Single instance connection.
	 */
	private static NetSIDDevConnection connection;

	private NetSIDDevConnection() {
	}

	public static final NetSIDDevConnection getInstance() {
		if (connection == null) {
			connection = new NetSIDDevConnection();
		}
		return connection;
	}

	/**
	 * Connect to server, if not already connected or connection changed
	 *
	 * @param hostname server host name
	 * @param port     server port address
	 * @throws IOException socket I/O error
	 */
	public void open(String hostname, int port) throws IOException {
		if (connection.isInvalidated()) {
			connection.close();
		}
		if (connection.isClosed()) {
			connection.connect(hostname, port);
		}
	}

	/**
	 * Open connection to server
	 *
	 * @param hostname server host name
	 * @param port     server port address
	 * @throws IOException socket I/O error
	 */
	private void connect(String hostname, int port) throws IOException {
		connectedSocket = new Socket();
		System.out.printf("Try to connect to NetworkSIDDevice: %s (%d)\n", hostname, port);
		connectedSocket.connect(new InetSocketAddress(hostname, port), SOCKET_CONNECT_TIMEOUT);
		System.out.printf("Connected to NetworkSIDDevice: %s (%d)\n", hostname, port);
	}

	/**
	 * Close the connection.
	 */
	public void close() {
		if (!isClosed()) {
			try {
				connectedSocket.close();
				System.out.println("Disconnected from NetworkSIDDevice");
			} catch (IOException e) {
				System.err.println("NetworkSIDDevice socket cannot be closed!");
			}
		}
		invalidated = false;
	}

	/**
	 * Connection has been closed?
	 *
	 * @return connection closed
	 */
	private boolean isClosed() {
		return connectedSocket == null || connectedSocket.isClosed();
	}

	/**
	 * Invalidate the connection.
	 */
	public void invalidate() {
		invalidated = true;
	}

	/**
	 * Connection has been invalidated?
	 *
	 * @return connection invalid, must be re-opened
	 */
	public boolean isInvalidated() {
		return invalidated;
	}

	/**
	 * Send byte array to server.
	 *
	 * @param cmd byte array to send
	 * @throws IOException socket I/O error
	 */
	public void send(byte[] cmd) throws IOException {
		connectedSocket.getOutputStream().write(cmd);
	}

	/**
	 * Receive byte from server.
	 *
	 * @return received byte
	 * @throws IOException socket I/O error
	 */
	public int receive() throws IOException {
		return connectedSocket.getInputStream().read();
	}

	/**
	 * Receive zero terminated byte array as string (ISO-88-59-1).
	 *
	 * @return string received from server.
	 * @throws IOException socket I/O error
	 */
	public String receiveString() throws IOException {
		int chIdx;
		for (chIdx = 0; chIdx < stringBytes.length; chIdx++) {
			connectedSocket.getInputStream().read(stringBytes, chIdx, 1);
			if (stringBytes[chIdx] <= 0) {
				break;
			}
		}
		return new String(stringBytes, 0, chIdx, ISO_8859_1);
	}

	public boolean isDisconnected() {
		return connection.isInvalidated() || connection.isClosed();
	}

}
