package libsidutils.vicesync;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ViceSync {

	private ServerSocket serverSocket;
	private DataInputStream in;
	private DataOutputStream out;
	public void connect(int port) throws IOException {
		int portNumber = port;
		serverSocket = new ServerSocket(portNumber);
		Socket clientSocket = serverSocket.accept();
		in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
		out = new DataOutputStream(clientSocket.getOutputStream());
	}

	public String receive() throws IOException {
		/*int type =*/ in.read();
		int length = in.readInt();
		byte[] result = new byte[length];
		in.read(result);
		return new String(result, "US-ASCII");
	}

	public void send(String answer) throws IOException {
		out.writeInt(answer.length());
		out.write(answer.getBytes());
	}

	public static class MOS6510State {
		long clk, syncClk;
		int pc, a, x, y, sp;

		public MOS6510State() {
		}

		public MOS6510State(long syncClk, int register_ProgramCounter, byte register_Accumulator, byte register_X,
				byte register_Y, byte register_StackPointer) {
			this.syncClk = syncClk;
			pc = register_ProgramCounter;
			a = register_Accumulator & 0xff;
			x = register_X & 0xff;
			y = register_Y & 0xff;
			sp = register_StackPointer & 0xff;
		}

		public long getClk() {
			return clk;
		}

		public long getSyncClk() {
			return syncClk;
		}
		@Override
		public boolean equals(Object obj) {
			MOS6510State other = (MOS6510State) obj;
			boolean ok = other != null && other.pc == pc && other.a == a && other.x == x && other.y == y
					&& other.sp == sp;
			if (!ok) {
				System.err.println("Difference detected!");
			}
			return ok;
		}

		@Override
		public int hashCode() {
			assert false : "hashCode not designed";
			return 42; // any arbitrary constant will do
		}
		
		@Override
		public String toString() {
			return String.format("%08d - %d: pc=%04x, a=%02x, x=%02x, y=%02x, sp=%02x", clk, syncClk, pc, (byte) a,
					(byte) x, (byte) y, (byte) sp);
		}
	}

	public MOS6510State getState(String viceState) {
		// clk=55292332, pc=0d9b, a=c8, x=20, y=04, sp=e8
		MOS6510State state = new MOS6510State();

		try (Scanner sc = new Scanner(viceState)) {
			sc.useDelimiter(",");
			while (sc.hasNext()) {
				String tk = sc.next().trim();
				int del = tk.indexOf('=');
				if (del != -1) {
					String reg = tk.substring(0, del);
					switch (reg) {
					case "clk":
						// clock
						state.clk = Long.parseLong(tk.substring(del + 1, tk.length()));
						break;
					case "syncClk":
						// relative clock
						state.syncClk = Long.parseLong(tk.substring(del + 1, tk.length()));
						break;
					case "pc":
						// Program counter
						state.pc = Integer.parseInt(tk.substring(del + 1, tk.length()), 16);
						break;
					case "a":
						// Accumulator
						state.a = Integer.parseInt(tk.substring(del + 1, tk.length()), 16);
						break;
					case "x":
						// X-Register
						state.x = Integer.parseInt(tk.substring(del + 1, tk.length()), 16);
						break;
					case "y":
						// Y-Register
						state.y = Integer.parseInt(tk.substring(del + 1, tk.length()), 16);
						break;
					case "sp":
						// Stack pointer
						state.sp = Integer.parseInt(tk.substring(del + 1, tk.length()), 16);
						break;

					default:
						break;
					}
				}
			}
		}
		return state;
	}

}
