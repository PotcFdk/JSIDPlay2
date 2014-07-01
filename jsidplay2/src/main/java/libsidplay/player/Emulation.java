package libsidplay.player;

public enum Emulation {
	/** No soundcard. Still allows wav generation */
	NONE,
	/** The following require a soundcard */
	RESID,
	/** The following require a soundcard */
	RESIDFP,
	/** The following should disable the soundcard */
	HARDSID
}