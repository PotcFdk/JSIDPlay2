package libsidutils.vicesync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ViceSync {

	private ServerSocket serverSocket;
	private BufferedReader in;
	private PrintWriter out;

	public void connect(int port) throws IOException {
		int portNumber = port;
		serverSocket = new ServerSocket(portNumber);
		Socket clientSocket = serverSocket.accept();
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		out = new PrintWriter(clientSocket.getOutputStream(), true);
	}

	public String receive() throws IOException {
		return in.readLine();
	}

	public void send(String answer) throws IOException {
		out.println(answer);
	}

	public static class MOS6510State {
		long clk;
		int pc, a, x, y, sp;

		public MOS6510State() {
		}

		public MOS6510State(int register_ProgramCounter, byte register_Accumulator, byte register_X, byte register_Y,
				byte register_StackPointer) {
			pc = register_ProgramCounter;
			a = register_Accumulator & 0xff;
			x = register_X & 0xff;
			y = register_Y & 0xff;
			sp = register_StackPointer & 0xff;
		}

		public long getClk() {
			return clk;
		}

		@Override
		public boolean equals(Object obj) {
			MOS6510State other = (MOS6510State) obj;
			boolean ok = other != null && other.pc == pc && other.a == a && other.x == x && other.y == y
					&& other.sp == sp;
			if (!ok) {
				System.err.println("!");
			}
			return ok;
		}

		@Override
		public String toString() {
			return String.format("%d: pc=%04x, a=%02x, x=%02x, y=%02x, sp=%02x", clk, pc, (byte) a, (byte) x, (byte) y,
					(byte) sp);
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
