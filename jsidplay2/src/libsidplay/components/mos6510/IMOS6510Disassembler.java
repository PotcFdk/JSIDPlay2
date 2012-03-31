package libsidplay.components.mos6510;

public interface IMOS6510Disassembler {

	String disassemble(int opcode, int instrOperand, int cycleEffectiveAddress);

}
