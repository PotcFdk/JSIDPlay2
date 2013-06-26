package libsidutils.cpuparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import libsidplay.components.mos6510.IMOS6510Disassembler;

public class CPUParser implements IMOS6510Disassembler {
	private static HashMap<Integer, CPUCommand> cpuCommands = new HashMap<Integer, CPUCommand>();
	private static CPUParser theInstance;

	static {
		try {
			parse();
		}
		catch (final Exception e) {
			// ExceptionInInitializers tend to get swallowed, so we dump the trace first
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	static public Map<Integer, CPUCommand> getCpuCommands() {
		return Collections.unmodifiableMap(cpuCommands);
	}

	public static final CPUParser getInstance() {
		if (theInstance == null) {
			theInstance = new CPUParser();
		}
		return theInstance;
	}
	
	/** Simple disassembler. Reads the hex bytes that constitute command,
	 * from supplied environment, and decodes the branch targets properly.
	 * TODO: improve this poor disassembler to something respectable
	 * 
	 * @return disassembly string like "LDA $12". */
	public String getDisassembly(final byte[] ram, final int instrAddress) {
		final CPUCommand cmd = cpuCommands.get(ram[instrAddress] & 0xff);
		String base = cmd.getCmd();
		if (cmd.getByteCount() == 2) {
			base += ":";
			final int nextByte = ram[instrAddress + 1 & 0xffff] & 0xff;
			if ((cmd.getOpCode() & 0x1F) == 0x10) { // Branch; replace by address
				base += String.format(cmd.getFormat(), nextByte, instrAddress + cmd.getByteCount() + (byte) nextByte & 0xffff);
			} else {
				base += String.format(cmd.getFormat(), nextByte);
			}

		} else if (cmd.getByteCount() == 3) {
			base += ":";
			base += String.format(cmd.getFormat(), ((
					ram[instrAddress + 2 & 0xffff] & 0xff) << 8)
					| (ram[instrAddress + 1 & 0xffff] & 0xff));
		}

		return base;
	}

	public String disassemble(final int opcode, final int operand, final int address) {
		final CPUCommand cmd = cpuCommands.get(opcode);
		String base = cmd.getCmd() + cmd.getAddressing();
		if (cmd.getByteCount() == 2) {
			if ((cmd.getOpCode() & 0x1F) == 0x10) { // Branch; replace by address
				base += String.format(cmd.getFormat(), operand & 0xff, address);
			} else {
				base += " " + String.format(cmd.getFormat(), operand & 0xff);
			}
		} else if (cmd.getByteCount() == 3) {
			base += " " + String.format(cmd.getFormat(), operand);
		}
		return base;
	}

	static private void parse() throws IOException {
		final InputStream is = CPUParser.class.getResourceAsStream("cpu.properties");
		final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		while (null != (line = reader.readLine())) {
			final String[] args = line.split(":");

			final int opCode = Integer.parseInt(args[0], 16);
			final String cmd = args[1];
			final String addressing = args[2];
			final String format = args[3];
			final int byteCount = Integer.parseInt(args[4]);

			final String cycles = args.length < 5 ? "" : args[4];
			final CPUCommand cpuCmd = new CPUCommand(opCode, cmd, addressing, format, byteCount, cycles);
			cpuCommands.put(opCode, cpuCmd);
		}
		reader.close();
	}

}