package libsidutils.disassembler;

public interface IMOS6510Disassembler {

	String disassemble(int opcode, int instrOperand, int cycleEffectiveAddress);

}
