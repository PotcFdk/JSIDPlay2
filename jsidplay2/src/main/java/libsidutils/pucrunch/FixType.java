package libsidutils.pucrunch;

public enum FixType {

    ftFastDisable,	/* -> 0x2c; bit $nnnn */

    ftOverlap,		/* overlap?(overlap-1):0 */
    ftOverlapLo,	/* (0x801 + (sizeof(headerUncrunch)-2+rleUsed-31)
			    + size - overlap) & 0xff; */
    ftOverlapHi,	/* (0x801 + (sizeof(headerUncrunch)-2+rleUsed-31)
			    + size - overlap) >> 8; */
    ftWrapCount,	/* (memend>>8) - ((endAddr + overlap - size) >> 8); */
    ftSizePages,	/* (size>>8) + 1 */
    ftSizeLo,		/* (0x801 + (sizeof(headerUncrunchNoWrap)-2+rleUsed-31)
			    + size - 0x100) & 0xff; */
    ftSizeHi,		/* (0x801+ (sizeof(headerUncrunchNoWrap)-2+rleUsed-31)
			    + size - 0x100) >> 8; */
    ftEndLo,		/* (endAddr - 0x100) & 0xff; */
    ftEndHi,		/* ((endAddr - 0x100) >> 8); */
    ftEscValue,		/* (escape>>(8-escBits)); */
    ftOutposLo,		/* (start & 0xff); */
    ftOutposHi,		/* (start >> 8); */
    ftEscBits,		/* escBits; */
    ftEsc8Bits,		/* 8-escBits; */
    ft1MaxGamma,	/* (1<<maxGamma); */
    ft8MaxGamma,	/* (8-maxGamma); */
    ft2MaxGamma,	/* (2<<maxGamma)-1; */
    ftExtraBits,	/* extraLZPosBits; */
    ftMemConfig,	/*header[0x8e1 -0x7ff] = memConfig; */
    ftCli,		/*header[0x8ef -0x7ff] = $58/$78 cli/sei; */
    ftExecLo,		/* (exec & 0xff); */
    ftExecHi,		/* (exec >> 8); */
    ftInposLo,		/* (endAddr - size) & 0xff; */
    ftInposHi,		/* ((endAddr - size) >> 8); */
    ftMaxGamma,		/* maxGamma + 1; */

    ftBEndLo,	/* basic end address */
    ftBEndHi,

    ftIBufferSize,	/* # of bytes in system input buffer $200- */
    ftStackSize,	/* # of bytes in zero page and stack $0f7- */
    ftDeCall,

    ftOp,		/* op for ADC-lz */

    ftReloc,
    ftEnd
}
