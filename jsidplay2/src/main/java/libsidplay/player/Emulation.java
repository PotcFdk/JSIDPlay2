package libsidplay.player;

public enum Emulation {
	/** No soundcard. Still allows wav generation */
	EMU_NONE,
	/** The following require a soundcard */
	EMU_RESID,
	/** The following should disable the soundcard */
	EMU_HARDSID
}