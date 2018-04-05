package libsidutils.assembler;

import java.util.Map;

public class KickAssemblerResult {

	private final byte[] data;

	private final Map<String, Integer> resolvedSymbols;

	public KickAssemblerResult(byte[] data, Map<String, Integer> resolvedSymbols) {
		this.data = data;
		this.resolvedSymbols = resolvedSymbols;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public Map<String, Integer> getResolvedSymbols() {
		return resolvedSymbols;
	}
}
