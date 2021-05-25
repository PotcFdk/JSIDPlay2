package builder.exsid;

public enum ChipSelect {
	/** 6581 */
	XS_CS_CHIP0,
	/** 8580 */
	XS_CS_CHIP1,
	/** Both chips. @warning Invalid for reads: undefined behaviour! */
	XS_CS_BOTH,
}
