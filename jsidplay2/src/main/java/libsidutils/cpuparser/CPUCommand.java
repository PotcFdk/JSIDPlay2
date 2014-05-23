package libsidutils.cpuparser;

public class CPUCommand {
	private final int opCode;
	private final String cmd;
	private final String addressing;
	private final String format;
	private final int byteCount;
	private final String cycles;

	public CPUCommand(final int opCode, final String cmd,
			final String addressing, final String format, final int byteCount,
			final String cycles) {
		this.opCode = opCode;
		this.cmd = cmd;
		this.addressing = addressing;
		this.format = format;
		this.byteCount = byteCount;
		this.cycles = cycles;
	}

	public int getOpCode() {
		return opCode;
	}

	public String getCmd() {
		return cmd;
	}

	public String getAddressing() {
		return addressing;
	}

	public String getFormat() {
		return format;
	}

	public int getByteCount() {
		return byteCount;
	}

	public String getCycles() {
		return cycles;
	}
}
