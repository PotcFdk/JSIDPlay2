package applet.disassembler;

public class CPUCommand {
	private int fOpCode;
	private String fCmd;
	private String fAddressing;
	private String fFormat;
	private int fByteCount;
	private String fCycles;
	
	public CPUCommand(int opCode, String cmd, String addressing, String format, int byteCount, String cycles) {
		fOpCode = opCode;
		fCmd = cmd;
		fAddressing = addressing;
		fFormat = format;
		fByteCount = byteCount;
		fCycles = cycles;
	}
	
	public int getOpCode() {
		return fOpCode;
	}
	
	public String getCmd() {
		return fCmd;
	}
	
	public String getAddressing() {
		return fAddressing;
	}
	
	public String getFormat() {
		return fFormat;
	}
	
	public int getByteCount() {
		return fByteCount;
	}

	public String getCycles() {
		return fCycles;
	}
}
