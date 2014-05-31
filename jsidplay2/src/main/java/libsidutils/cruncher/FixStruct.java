package libsidutils.pucrunch;

public class FixStruct {
	public FixStruct(byte[] headerc64, int length, FixEntry[] fixtablec64,
			String string, int fixfC64) {
		code = headerc64;
		codeSize = length;
		fixes = fixtablec64;
		name = string;
		flags = fixfC64;
	}

	byte[] code;
	int codeSize;
	FixEntry[] fixes;
	String name;
	int flags;

}
