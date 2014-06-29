package libsidutils.cruncher;

public interface IHeader {

	final static int FIXF_C64 = 1;
	final static int FIXF_VIC20 = 2;
	final static int FIXF_C16 = 4;
	final static int FIXF_C128 = 8;
	final static int FIXF_MACHMASK = 0xff; /* Must be exactly correct */

	final static int FIXF_WRAP = 256; /* If requested, must be present */
	final static int FIXF_DLZ = 512; /* If requested, must be present */
	final static int FIXF_BASIC = 1024; /* If requested, must be present */

	final static int FIXF_FAST = 2048;
	final static int FIXF_SHORT = 4096;

	final static int FIXF_MUSTMASK = (FIXF_WRAP | FIXF_DLZ | FIXF_BASIC);

	FixStruct fixStruct[] = {
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64.asm",
					"C64", FIXF_C64),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64S.asm",
					"C64 short", FIXF_C64 | FIXF_SHORT),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64SB.asm",
					"C64 short basic", FIXF_C64 | FIXF_SHORT | FIXF_BASIC),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64SW.asm",
					"C64 short wrap", FIXF_C64 | FIXF_SHORT | FIXF_WRAP),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64D.asm",
					"C64 delta", FIXF_C64 | FIXF_DLZ),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64F.asm",
					"C64 fast", FIXF_C64 | FIXF_FAST),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64W.asm",
					"C64 wrap", FIXF_C64 | FIXF_WRAP),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64WD.asm",
					"C64 wrap delta", FIXF_C64 | FIXF_WRAP | FIXF_DLZ),
			new FixStruct("/libsidutils/cruncher/PUCrunch_headerC64WF.asm",
					"C64 fast wrap", FIXF_C64 | FIXF_WRAP | FIXF_FAST), };
}
