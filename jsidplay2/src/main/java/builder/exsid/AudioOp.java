package builder.exsid;

public enum AudioOp {
	/** mix: 6581 L / 8580 R */
	XS_AU_6581_8580,
	/** mix: 8580 L / 6581 R */
	XS_AU_8580_6581,
	/** mix: 8580 L and R */
	XS_AU_8580_8580,
	/** mix: 6581 L and R */
	XS_AU_6581_6581,
	/** mute output */
	XS_AU_MUTE,
	/** unmute output */
	XS_AU_UNMUTE,
}
