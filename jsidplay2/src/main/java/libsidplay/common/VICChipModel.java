package libsidplay.common;

/**
 * Chip models supported by MOS656X.
 */
public enum VICChipModel {
	/** Old NTSC */
	MOS6567R56A(
			new float[] { 560, 1825, 840, 1500, 1180, 1180, 840, 1500, 1180, 840, 1180, 840, 1180, 1500, 1180, 1500 }),
	/** NTSC */
	MOS6567R8(
			new float[] { 590, 1825, 950, 1380, 1030, 1210, 860, 1560, 1030, 860, 1210, 950, 1160, 1560, 1160, 1380 }),
	/** PAL */
	MOS6569R1(
			new float[] { 630, 1850, 900, 1560, 1260, 1260, 900, 1560, 1260, 900, 1260, 900, 1260, 1560, 1260, 1560 }),
	/** PAL */
	MOS6569R3(new float[] { 700, 1850, 1090, 1480, 1180, 1340, 1020, 1620, 1180, 1020, 1340, 1090, 1300, 1620, 1300,
			1480 }),
	/** PAL */
	MOS6569R4(new float[] { 500, 1875, 840, 1300, 920, 1100, 760, 1500, 920, 760, 1100, 840, 1050, 1500, 1050, 1300 }),
	/** PAL */
	MOS6569R5(new float[] { 540, 1850, 900, 1340, 980, 1150, 810, 1520, 980, 810, 1150, 900, 1110, 1520, 1110, 1340 });

	private float[] luminances;

	private VICChipModel(float[] luminances) {
		this.luminances = luminances;
	}

	/**
	 * Voltage by color (0x0-0x0F)
	 * 
	 * <pre>
	 * http://www.zimmers.net/anonftp/pub/cbm/documents/chipdata/656x-luminances.txt
	 * </pre>
	 */
	public float[] getLuminances() {
		return luminances;
	}
}