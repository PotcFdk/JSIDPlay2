/* Pucrunch ©1997-2008 by Pasi 'Albert' Ojala, a1bert@iki.fi */
/* Pucrunch is now under LGPL: see the doc for details. */
package libsidutils.cruncher;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import libsidutils.assembler.KickAssembler;

public class PUCrunch implements IHeader {

	private static final boolean VERBOSE = false;

	private static final int F_STATS = (1 << 1);
	private static final int F_AUTO = (1 << 2);
	private static final int F_NOOPT = (1 << 3);
	private static final int F_AUTOEX = (1 << 4);
	private static final int F_2MHZ = (1 << 6);

	private static final int LITERAL = 0;
	private static final int LZ77 = 1;
	private static final int RLE = 2;
	private static final int DLZ = 3;
	private static final int MMARK = 4;

	private static final int F_NORLE = (1 << 9);
	private static final int OUT_SIZE = 2000000;

	private int maxGamma = 7, reservedBytes = 2, escBits = 2, escMask = 0xc0,
			extraLZPosBits = 0, rleUsed = 15, memConfig = 0x37,
			cliConfig = 0x58, lrange, maxlzlen, maxrlelen, outPointer = 0,
			bitMask = 0x80, timesDLz = 0, inlen, lzopt = 0;

	/**
	 * 0..125, 126 -> 1..127
	 */
	private int LRANGE = (((2 << maxGamma) - 3) * 256);
	private int MAXLZLEN = (2 << maxGamma);
	/**
	 * 0..126 -> 1..127
	 */
	private int MAXRLELEN = (((2 << maxGamma) - 2) * 256);
	private int DEFAULT_LZLEN = LRANGE;

	private byte[] outBuffer = new byte[OUT_SIZE], indata, newesc;

	private int lenValue[] = new int[256], rleHist[] = new int[256],
			rleLen[] = new int[256];

	private int lenStat[][] = new int[8][4];

	private byte rleValues[] = { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private int[] rle, elr, lzlen, lzpos, lzmlen, lzmpos, lzlen2, lzpos2, mode,
			backSkip, length;

	/* Non-recursive version */
	/* NOTE! IMPORTANT! the "length" array length must be inlen+1 */

	private final KickAssembler assembler = new KickAssembler();
	private Map<String, Integer> labels;

	/**
	 * <PRE>
	 * 	-------->
	 * 	    z..zx.....x						     normal (zz != ee)
	 * 	    e..e	value(LEN)	value(POSHI+1)	8+b(POSLO)   LZ77
	 * 	    e..e	0    (2)	0 (2-256)	8b(POSLO)    LZ77
	 * 	    e..e	100  (3)	111111 111111		     END of FILE
	 * 	#ifdef DELTA
	 * 	    e..e	101  (4..)	111111 111111	8b(add) 8b(POSLO)	DLZ
	 * 	#endif
	 * 
	 * 	    e..e010	n..ne.....e				     escape + new esc
	 * 	    e..e011	value(LEN)	bytecode		     Short RLE  2..
	 * 	    e..e011	111..111 8b(LENLO) value(LENHI+1) bytecode   Long RLE
	 * 			(values 64.. not used (may not be available) in bytecode)
	 * 
	 * 
	 * 	e..e011 0 0			RLE=2, rank 1 (saves 11.. bit)
	 * 	e..e011 0 10 x			RLE=2, rank 2-3 (saves 9.. bit)
	 * 	e..e011 0 11 0xx		RLE=2, rank 4-7 (saves 7.. bit)
	 * 	e..e011 0 11 10xxx		RLE=2, rank 8-15 (saves 5.. bit)
	 * 	e..e011 0 11 110xxxx xxxx	RLE=2, not ranked
	 * 
	 * 
	 * 	LZ77, len=2 (pos<=256) saves 4 bits (2-bit escape)
	 * 	LZ77, len=3 saves 10..1 bits (pos 2..15616)
	 * 	LZ77, len=4 saves 18..9 bits
	 * 	LZ77, len=5 saves 24..15 bits
	 * 
	 * 	RLE, len=2 saves 11..1(..-5) bits (bytecode rank 1..not ranked)
	 * 	RLE, len=3 saves 15..2 bits
	 * 	RLE, len=4 saves 23..10 bits
	 * 	RLE, len=5 saves 29..16 bits
	 * 
	 * 	bs: 3505 LZ reference points, 41535 bytes -> 11.85, i.e. 8.4% referenced
	 * 
	 * 
	 * 	 1) Short RLE -> gamma + 1 linear bit -> ivanova.run -29 bytes
	 * 
	 * 	 2) ?? .. no
	 * 	    esc = RLE, with value 1
	 * 	    e..e01 value(1)	n..ne.....e			     escape + new esc
	 * 	    e..e01 value(LEN)	bytecode			     Short RLE  2..
	 * 	    e..e01 111..111 8b(LENLO) value(LENHI+1) bytecode        Long RLE
	 * 			(values 64.. not used (may not be available) in bytecode)
	 * 
	 * 
	 * </PRE>
	 */

	/**
	 * <PRE>
	 * 	Value:
	 * 
	 * 	Elias Gamma Code rediscovered, just the prefix bits are reversed, plus
	 * 	there is a length limit (1 bit gained for each value in the last group)
	 * 	; 0000000	not possible
	 * 	; 0000001	0		1			-6 bits
	 * 	; 000001x	10	x	2-3			-4 bits
	 * 	; 00001xx	110 	xx	4-7			-2 bits
	 * 	; 0001xxx	1110 	xxx	8-15			+0 bits
	 * 	; 001xxxx	11110	xxxx	16-31			+2 bits
	 * 	; 01xxxxx	111110	xxxxx	32-63			+4 bits
	 * 	; 1xxxxxx	111111	xxxxxx	64-127			+5 bits
	 * </PRE>
	 */

	private FixStruct BestMatch(int type) {
		int dc = 0;
		FixStruct best = null;

		while (dc < fixStruct.length) {
			if ((fixStruct[dc].flags & FIXF_MACHMASK) == (type & FIXF_MACHMASK)) {
				/* machine is correct */
				/* Require wrap if necessary, allow wrap if not */
				/* Require delta matches */
				if (((fixStruct[dc].flags & type) & FIXF_MUSTMASK) == (type & FIXF_MUSTMASK)) {

					/* Haven't found any match or this is better */
					if (null == best
							|| ((type & FIXF_WRAP) == (fixStruct[dc].flags & FIXF_WRAP) && (0 == (type & (FIXF_FAST | FIXF_SHORT)) || (fixStruct[dc].flags
									& type & (FIXF_FAST | FIXF_SHORT)) != 0)))
						best = fixStruct[dc];
					/* If requirements match exactly, can return */
					/* Assumes that non-wraps are located before wrap versions */
					if ((type & (FIXF_FAST | FIXF_SHORT)) == (fixStruct[dc].flags & (FIXF_FAST | FIXF_SHORT))) {
						return fixStruct[dc];
					}
				}
			}
			dc++;
		}
		return best;
	}

	private static class IntContainer {
		public IntContainer(int v) {
			intVal = v;
		}

		int intVal;
	}

	private void SavePack(int type, byte[] data, int size, PrintStream fp,
			int start, int exec, int escape, byte[] rleValues, int endAddr,
			int progEnd, int extraLZPosBits, boolean enable2MHz, int memStart,
			int memEnd) {
		FixStruct dc;
		int i, overlap = 0, stackUsed = 0, ibufferUsed = 0;

		if ((memStart & 0xff) != 1) {
			throw new RuntimeException(String.format(
					"Misaligned basic start 0x%04x\n", memStart));
		} else if (memStart > 9999) {
			/* The basic line only holds 4 digits.. */
			throw new RuntimeException(String.format(
					"Too high basic start 0x%04x\n", memStart));
		}

		if (endAddr > memEnd) {
			overlap = endAddr - memEnd;
			endAddr = memEnd;

			/*
			 * Make the decrunch code wrap from $ffff to $004b. The decrunch
			 * code first copies the data that would exceed $ffff to $004b and
			 * then copy the rest of it to end at $ffff.
			 */

			if (overlap > 22) {
				System.err.printf("Warning: data overlap is %d, but only 22 "
						+ "is totally safe!\n", overlap);
				System.err.printf(
						"The data from $61 to $%02x is overwritten.\n",
						0x4b + overlap);
			}
		}
		if (overlap != 0) {
			type |= FIXF_WRAP;
		} else {
			type &= ~FIXF_WRAP;
		}
		dc = BestMatch(type);
		if (null == dc) {
			throw new RuntimeException("No matching decompressor found\n");
		}
		HashMap<String, String> globals = new HashMap<String, String>();
		globals.put("pc", String.valueOf(memStart));
		globals.put("ftFastDisable", String.valueOf(enable2MHz ? 1 : 0));
		globals.put("ftOverlap",
				String.valueOf(overlap != 0 ? (overlap - 1) : 0));
		globals.put("ftOverlapAddr",
				String.valueOf(rleUsed - 15 + size - overlap));
		globals.put("ftSizePages", String.valueOf((size >> 8) + 1));
		globals.put("ftSizeAddr",
				String.valueOf(rleUsed - 15 + size - 0x100 - overlap));
		globals.put("ftEndAddr", String.valueOf(endAddr - 0x100));
		globals.put("ftEscValue", String.valueOf(escape >> (8 - escBits)));
		globals.put("ftOutposAddr", String.valueOf(start));
		globals.put("ftEscBits", String.valueOf(escBits));
		globals.put("ftMaxGamma", String.valueOf(maxGamma));
		globals.put("ftExtraBits", String.valueOf(extraLZPosBits));
		globals.put("ftMemConfig", String.valueOf(memConfig));
		globals.put("ftCli", String.valueOf(cliConfig));
		globals.put("ftExec", String.valueOf(exec));
		globals.put("ftInpos", String.valueOf(endAddr + overlap - size));
		globals.put("ftBEndAddr", String.valueOf(progEnd));
		InputStream asm = PUCrunch.class.getResourceAsStream(dc.resourceName);
		byte[] header = assembler.assemble(dc.resourceName, asm, globals);
		labels = assembler.getLabels();
		if (0 == memStart)
			memStart = 0x801;

		stackUsed = labels.get("ftStackSize");
		ibufferUsed = labels.get("ftIBufferSize") != null ? labels
				.get("ftIBufferSize") : 0;

		for (i = 1; i <= 15; i++)
			header[header.length - 15 + i - 1] = rleValues[i];

		System.out.printf("Saving %s\n", dc.name);
		fp.write(header, 0, header.length + rleUsed - 15);
		fp.write(data, 0, size);
		if ((dc.flags & FIXF_SHORT) != 0) {
			System.out.printf("Uses the memory $2d-$30, ");
		} else {
			System.out.printf("Uses the memory $2d/$2e, ");
		}
		if (overlap != 0)
			System.out.printf("$4b-$%02x, ", 0x4b + overlap);
		else if ((dc.flags & FIXF_WRAP) != 0)
			System.out.printf("$4b, ");
		if (stackUsed != 0)
			System.out.printf("$f7-$%x, ", 0xf7 + stackUsed);
		if (ibufferUsed != 0)
			System.out.printf("$200-$%x, ", 0x200 + ibufferUsed);
		System.out.printf("and $%04x-$%04x.\n", (start < memStart + 1) ? start
				: memStart + 1, endAddr - 1);
	}

	private void FlushBits() {
		if (bitMask != 0x80)
			outPointer++;
	}

	private void PutBit(int bit) {
		if (bit != 0 && outPointer < OUT_SIZE)
			outBuffer[outPointer] |= bitMask;
		bitMask >>= 1;
		if (0 == bitMask) {
			bitMask = 0x80;
			outPointer++;
		}
	}

	private void PutValue(int value) {
		int bits = 0, count = 0;

		while (value > 1) {
			bits = (bits << 1) | (value & 1); /* is reversed compared to value */
			value >>= 1;
			count++;
			PutBit(1);
		}
		if (count < maxGamma)
			PutBit(0);
		while ((count--) != 0) {
			PutBit((bits & 1)); /* output is reversed again -> same as value */
			bits >>= 1;
		}
	}

	private int RealLenValue(int value) {
		int count = 0;

		if (value < 2) /* 1 */
			count = 0;
		else if (value < 4) /* 2-3 */
			count = 1;
		else if (value < 8) /* 4-7 */
			count = 2;
		else if (value < 16) /* 8-15 */
			count = 3;
		else if (value < 32) /* 16-31 */
			count = 4;
		else if (value < 64) /* 32-63 */
			count = 5;
		else if (value < 128) /* 64-127 */
			count = 6;
		else if (value < 256) /* 128-255 */
			count = 7;

		if (count < maxGamma)
			return 2 * count + 1;
		return 2 * count;
	}

	private void InitValueLen() {
		int i;
		for (i = 1; i < 256; i++)
			lenValue[i] = RealLenValue(i);
	}

	private void PutNBits(int b, int bits) {
		while ((bits--) != 0)
			PutBit((b & (1 << bits)));
	}

	private void OutputNormal(IntContainer esc, byte[] data, int dataPos,
			int newesc) {
		if ((data[dataPos + 0] & escMask) == esc.intVal) {
			PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */
			PutValue(2 - 1);
			PutBit(1);
			PutBit(0);

			esc.intVal = newesc;
			PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */
			PutNBits((data[dataPos + 0] & 0xff), 8 - escBits);

			return;
		}
		PutNBits((data[dataPos + 0] & 0xff), 8);
	}

	private void OutputEof(IntContainer esc) {
		/* EOF marker */
		PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */
		PutValue(3 - 1); /* >1 */
		PutValue((2 << maxGamma) - 1); /* Maximum value */

		/* flush */
		FlushBits();
	}

	private void PutRleByte(int data) {
		int index;

		for (index = 1; index < 16/* 32 */; index++) {
			if (data == (rleValues[index] & 0xff)) {
				if (index == 1)
					lenStat[0][3]++;
				else if (index <= 3)
					lenStat[1][3]++;
				else if (index <= 7)
					lenStat[2][3]++;
				else if (index <= 15)
					lenStat[3][3]++;
				/*
				 * else if (index<=31) lenStat[4][3]++;
				 */

				PutValue(index);
				return;
			}
		}
		/* System.out.printf("RLECode n: 0x%02x\n", data); */
		PutValue(16/* 32 */+ (data >> 4/* 3 */));

		PutNBits(data, 4/* 3 */);

		lenStat[4/* 5 */][3]++;
		/* Note: values 64..127 are not used if maxGamma>5 */
	}

	private void InitRleLen() {
		int i;

		for (i = 0; i < 256; i++)
			rleLen[i] = lenValue[16/* 32 */+ 0] + 4/* 3 */;
		for (i = 1; i < 16 /* 32 */; i++)
			rleLen[rleValues[i] & 0xff] = lenValue[i];
	}

	private int LenRle(int len, int data) {
		int out = 0;

		do {
			if (len == 1) {
				out += escBits + 3 + 8;
				len = 0;
			} else if (len <= (1 << maxGamma)) {
				out += escBits + 3 + lenValue[len - 1] + rleLen[data];
				len = 0;
			} else {
				int tmp = Math.min(len, maxrlelen);
				out += escBits + 3 + maxGamma + 8
						+ lenValue[((tmp - 1) >> 8) + 1] + rleLen[data];

				len -= tmp;
			}
		} while (len != 0);
		return out;
	}

	private void OutputRle(IntContainer esc, byte[] data, int dataPos,
			int rlelen) {
		int len = rlelen;

		while (len != 0) {
			if (len >= 2 && len <= (1 << maxGamma)) {
				/* Short RLE */
				if (len == 2)
					lenStat[0][2]++;
				else if (len <= 4)
					lenStat[1][2]++;
				else if (len <= 8)
					lenStat[2][2]++;
				else if (len <= 16)
					lenStat[3][2]++;
				else if (len <= 32)
					lenStat[4][2]++;
				else if (len <= 64)
					lenStat[5][2]++;
				else if (len <= 128)
					lenStat[6][2]++;
				else if (len <= 256)
					lenStat[6][2]++;

				PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */
				PutValue(2 - 1);
				PutBit(1);
				PutBit(1);
				PutValue(len - 1);
				PutRleByte(data[dataPos] & 0xff);
				return;
			}
			if (len < 3) {
				while (len-- != 0)
					OutputNormal(esc, data, dataPos, esc.intVal);
				return;
			}

			if (len <= maxrlelen) {
				/* Run-length encoding */
				PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */

				PutValue(2 - 1);
				PutBit(1);
				PutBit(1);

				PutValue((1 << maxGamma)
						+ (((len - 1) & 0xff) >> (8 - maxGamma)));

				PutNBits((len - 1), 8 - maxGamma);
				PutValue(((len - 1) >> 8) + 1);
				PutRleByte(data[dataPos] & 0xff);

				return;
			}

			/* Run-length encoding */
			PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */

			PutValue(2 - 1);
			PutBit(1);
			PutBit(1);

			PutValue((1 << maxGamma)
					+ (((maxrlelen - 1) & 0xff) >> (8 - maxGamma)));

			PutNBits((maxrlelen - 1) & 0xff, 8 - maxGamma);
			PutValue(((maxrlelen - 1) >> 8) + 1);
			PutRleByte(data[dataPos]);

			len -= maxrlelen;
			dataPos += maxrlelen;
		}
		return;
	}

	/* e..e 101 (4..) 111111 111111 8b(add) 8b(POSLO) DLZ */
	private int LenDLz(int lzlen, int lzpos) {
		return escBits + 2 * maxGamma + 8 + 8 + lenValue[lzlen - 1];
	}

	private void OutputDLz(IntContainer esc, int lzlen, int lzpos, int add) {
		PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */

		PutValue(lzlen - 1);
		PutValue((2 << maxGamma) - 1); /* Maximum value */
		PutNBits(add, 8);
		PutNBits(((lzpos - 1) & 0xff) ^ 0xff, 8);

		timesDLz++;
	}

	private int LenLz(int lzlen, int lzpos) {
		if (lzlen == 2) {
			if (lzpos <= 256)
				return escBits + 2 + 8;
			return 100000;
		}
		return escBits + 8 + extraLZPosBits
				+ lenValue[((lzpos - 1) >> (8 + extraLZPosBits)) + 1] + lzlen < 257 ? lenValue[lzlen - 1]
				: 50;
		// Bug in the C-Version! lzlen==257
	}

	private void OutputLz(IntContainer esc, int lzlen, int lzpos, byte[] data,
			int dataPos, int curpos) {
		if (lzlen == 2)
			lenStat[0][1]++;
		else if (lzlen <= 4)
			lenStat[1][1]++;
		else if (lzlen <= 8)
			lenStat[2][1]++;
		else if (lzlen <= 16)
			lenStat[3][1]++;
		else if (lzlen <= 32)
			lenStat[4][1]++;
		else if (lzlen <= 64)
			lenStat[5][1]++;
		else if (lzlen <= 128)
			lenStat[6][1]++;
		else if (lzlen <= 256)
			lenStat[7][1]++;

		if (lzlen >= 2 && lzlen <= maxlzlen) {
			int tmp;

			PutNBits((esc.intVal >> (8 - escBits)), escBits); /* escBits>=0 */

			tmp = ((lzpos - 1) >> (8 + extraLZPosBits)) + 2;
			if (tmp == 2)
				lenStat[0][0]++;
			else if (tmp <= 4)
				lenStat[1][0]++;
			else if (tmp <= 8)
				lenStat[2][0]++;
			else if (tmp <= 16)
				lenStat[3][0]++;
			else if (tmp <= 32)
				lenStat[4][0]++;
			else if (tmp <= 64)
				lenStat[5][0]++;
			else if (tmp <= 128)
				lenStat[6][0]++;
			else if (tmp <= 256)
				lenStat[6][0]++;

			if (lzlen == 2) {
				PutValue(lzlen - 1);
				PutBit(0);
				if (lzpos > 256)
					System.err.printf(
							"Error at %d: lzpos too long (%d) for lzlen==2\n",
							curpos, lzpos);
				PutNBits(((lzpos - 1) & 0xff) ^ 0xff, 8);
			} else {
				PutValue(lzlen - 1);
				PutValue(((lzpos - 1) >> (8 + extraLZPosBits)) + 1);
				PutNBits(((lzpos - 1) >> 8), extraLZPosBits);
				PutNBits(((lzpos - 1) & 0xff) ^ 0xff, 8);
			}
			return;
		}
		System.err.printf("Error: lzlen too short/long (%d)\n", lzlen);
	}

	private int OptimizeLength(int optimize) {
		int i;

		length[inlen] = 0; /* one off the end, our 'target' */
		for (i = inlen - 1; i >= 0; i--) {
			int r1 = 8 + length[i + 1], r2, r3;

			if (0 == lzlen[i] && 0 == rle[i]
					&& (null == lzlen2 || 0 == lzlen2[i])

			) {
				length[i] = r1;
				mode[i] = LITERAL;
				continue;
			}

			/* If rle>maxlzlen, skip to the start of the rle-maxlzlen.. */
			if (rle[i] > maxlzlen && elr[i] > 1) {
				int z = elr[i];

				i -= elr[i];

				r2 = LenRle(rle[i], (indata[i] & 0xff)) + length[i + rle[i]];
				if (optimize != 0) {
					int ii, mini = rle[i], minv = r2;

					int bot = rle[i] - (1 << maxGamma);
					if (bot < 2)
						bot = 2;

					for (ii = mini - 1; ii >= bot; ii--) {
						int v = LenRle(ii, (indata[i] & 0xff)) + length[i + ii];
						if (v < minv) {
							minv = v;
							mini = ii;
						}
					}
					if (minv != r2) {
						lzopt += r2 - minv;
						rle[i] = mini;
						r2 = minv;
					}
				}
				length[i] = r2;
				mode[i] = RLE;

				for (; z >= 0; z--) {
					length[i + z] = r2;
					mode[i + z] = RLE;
				}
				continue;
			}
			r3 = r2 = r1 + 1000; /* r3 >= r2 > r1 */

			if (rle[i] != 0) {
				r2 = LenRle(rle[i], (indata[i] & 0xff)) + length[i + rle[i]];

				if (optimize != 0) {
					int ii, mini = rle[i], minv = r2;

					/*
					 * Check only the original length and all shorter lengths
					 * that are power of two.
					 * 
					 * Does not really miss many 'minimums' this way, at least
					 * not globally..
					 * 
					 * Makes the assumption that the Elias Gamma Code is used,
					 * i.e. values of the form 2^n are 'optimal'
					 */
					ii = 2;
					while (rle[i] > ii) {
						int v = LenRle(ii, (indata[i] & 0xff)) + length[i + ii];
						if (v < minv) {
							minv = v;
							mini = ii;
						}
						ii <<= 1;
					}
					if (minv != r2) {
						/* printf("%05d RL %d %d\n", i, rle[i], mini); */
						lzopt += r2 - minv;
						rle[i] = mini;
						r2 = minv;
					}
				}
			}
			if (lzlen[i] != 0) {
				r3 = LenLz(lzlen[i], lzpos[i]) + length[i + lzlen[i]];

				if (optimize != 0 && lzlen[i] > 2) {
					int ii, mini = lzlen[i], minv = r3, mino = lzpos[i];
					int topLen = LenLz(lzlen[i], lzpos[i])
							- lenValue[lzlen[i] - 1];

					/*
					 * Check only the original length and all shorter lengths
					 * that are power of two.
					 * 
					 * Does not really miss many 'minimums' this way, at least
					 * not globally..
					 * 
					 * Makes the assumption that the Elias Gamma Code is used,
					 * i.e. values of the form 2^n are 'optimal'
					 */
					ii = 4;
					while (lzlen[i] > ii) {
						int v = topLen + lenValue[ii - 1] + length[i + ii];
						if (v < minv) {
							minv = v;
							mini = ii;
						}
						ii <<= 1;
					}

					/*
					 * Then check the max lengths we have found, but did not
					 * originally approve because they seemed to gain less than
					 * the shorter, nearer matches.
					 */
					ii = 3;
					while (lzmlen[i] >= ii) {
						int v = LenLz(ii, lzmpos[i]) + length[i + ii];
						if (v < minv) {
							minv = v;
							mini = ii;
							mino = lzmpos[i];
						}
						ii++;
					}
					/*
					 * Note: 2-byte optimization checks are no longer done with
					 * the rest, because the equation gives too long code
					 * lengths for 2-byte matches if extraLzPosBits>0.
					 */
					/* Two-byte rescan/check */
					if (backSkip[i] != 0 && backSkip[i] <= 256) {
						/* There are previous occurrances (near enough) */
						int v = LenLz(2, backSkip[i]) + length[i + 2];

						if (v < minv) {
							minv = v;
							mini = 2;
							lzlen[i] = mini;
							r3 = minv;
							lzpos[i] = backSkip[i];
						}
					}
					if (minv != r3 && minv < r2) {
						/*
						 * printf("@%05d LZ %d %4x -> %d %4x\n", i, lzlen[i],
						 * lzpos[i], mini, lzpos[i]);
						 */
						lzopt += r3 - minv;
						lzlen[i] = mini;
						lzpos[i] = mino;
						r3 = minv;
					}
				}
			}

			if (r2 <= r1) {
				if (r2 <= r3) {
					length[i] = r2;
					mode[i] = RLE;
				} else {
					length[i] = r3;
					mode[i] = LZ77;
				}
			} else {
				if (r3 <= r1) {
					length[i] = r3;
					mode[i] = LZ77;
				} else {
					length[i] = r1;
					mode[i] = LITERAL;
				}
			}
			if (lzlen2 != null && lzlen2[i] > 3) {
				r3 = LenDLz(lzlen2[i], lzpos2[i]) + length[i + lzlen2[i]];
				if (r3 < length[i]) {
					length[i] = r3;
					mode[i] = DLZ;
				}
			}
		}
		return length[0];
	}

	/**
	 * <PRE>
	 *     The algorithm in the OptimizeEscape() works as follows:
	 *     1) Only unpacked bytes are processed, they are marked
	 *        with MMARK. We proceed from the end to the beginning.
	 *        Variable A (old/new length) is updated.
	 *     2) At each unpacked byte, one and only one possible
	 *        escape matches. A new escape code must be selected
	 *        for this case. The optimal selection is the one which
	 *        provides the shortest number of escapes to the end
	 *        of the file,
	 * 	i.e. A[esc] = 1+min(A[0], A[1], .. A[states-1]).
	 *        For other states A[esc] = A[esc];
	 *        If we change escape in this byte, the new escape is
	 *        the one with the smallest value in A.
	 *     3) The starting escape is selected from the possibilities
	 *        and mode 0 is restored to all mode 3 locations.
	 * 
	 * </PRE>
	 */

	private int OptimizeEscape(IntContainer startEscape, IntContainer nonNormal) {
		int i, j, states = (1 << escBits);
		int minp = 0, minv = 0, other = 0;
		int a[] = new int[256]; /* needs int/long */
		int b[] = new int[256]; /* Remembers the # of escaped for each */
		int esc8 = 8 - escBits;

		for (i = 0; i < 256; i++)
			b[i] = a[i] = -1;

		if (states > 256) {
			System.err.printf("Escape optimize: only 256 states (%d)!\n",
					states);
			return 0;
		}

		/* Mark those bytes that are actually outputted */
		for (i = 0; i < inlen;) {
			switch (mode[i]) {
			// if (DELTA) {
			case DLZ:
				other++;
				i += lzlen2[i];
				break;
			// }

			case LZ77:
				other++;
				i += lzlen[i];
				break;

			case RLE:
				other++;
				i += rle[i];
				break;

			/* case LITERAL: */
			default:
				mode[i++] = MMARK; /* mark it used so we can identify it */
				break;
			}
		}

		for (i = inlen - 1; i >= 0; i--) {
			/* Using a table to skip non-normal bytes does not help.. */
			if (mode[i] == MMARK) {
				int k = ((indata[i] & 0xff) >> esc8);

				/* Change the tag values back to normal */
				mode[i] = LITERAL;

				/*
				 * k are the matching bytes, minv is the minimum value, minp is
				 * the minimum index
				 */

				newesc[i] = (byte) (minp << esc8);
				a[k] = minv + 1;
				b[k] = b[minp] + 1;
				if (k == minp) {
					/* Minimum changed -> need to find a new minimum */
					/* a[k] may still be the minimum */
					minv++;
					for (k = states - 1; k >= 0; k--) {
						if (a[k] < minv) {
							minv = a[k];
							minp = k;
							/*
							 * There may be others, but the first one that is
							 * smaller than the old minimum is equal to any
							 * other new minimum.
							 */
							break;
						}
					}
				}
			}
		}

		/* Select the best value for the initial escape */
		if (startEscape != null) {
			i = inlen; /* make it big enough */
			for (j = states - 1; j >= 0; j--) {
				if (a[j] <= i) {
					startEscape.intVal = (j << esc8);
					i = a[j];
				}
			}
		}
		if (nonNormal != null)
			nonNormal.intVal = other;
		return b[startEscape != null ? (startEscape.intVal >> esc8) : 0];
	}

	/* Initialize the RLE byte code table according to all RLE's found so far */
	/* O(n) */
	private void InitRle(int flags) {
		int p, mr, mv, i;

		for (i = 1; i < 16/* 32 */; i++) {
			mr = -1;
			mv = 0;

			for (p = 0; p < 256; p++) {
				if (rleHist[p] > mv) {
					mv = rleHist[p];
					mr = p;
				}
			}
			if (mv > 0) {
				rleValues[i] = (byte) mr;
				rleHist[mr] = -1;
			} else
				break;
		}
		InitRleLen();
	}

	/* Initialize the RLE byte code table according to RLE's actually used */
	/* O(n) */
	private void OptimizeRle(int flags) {
		int p, mr, mv, i;

		if ((flags & F_NORLE) != 0) {
			rleUsed = 0;
			return;
		}
		if ((flags & F_STATS) != 0)
			System.out.printf("RLE Byte Code Re-Tune, RLE Ranks:\n");
		for (p = 0; p < 256; p++)
			rleHist[p] = 0;

		for (p = 0; p < inlen;) {
			switch (mode[p]) {
			// if (DELTA) {
			case DLZ: /* lz */
				p += lzlen2[p];
				break;
			// }
			case LZ77: /* lz */
				p += lzlen[p];
				break;

			case RLE: /* rle */
				rleHist[indata[p] & 0xff]++;
				p += rle[p];
				break;

			/*
			 * case LITERAL: case MMARK:
			 */
			default:
				p++;
				break;
			}
		}

		for (i = 1; i < 16 /* 32 */; i++) {
			mr = -1;
			mv = 0;

			for (p = 0; p < 256; p++) {
				if (rleHist[p] > mv) {
					mv = rleHist[p];
					mr = p;
				}
			}
			if (mv > 0) {
				rleValues[i] = (byte) mr;
				if ((flags & F_STATS) != 0) {
					System.out.printf(" %2d.0x%02x %-3d ", i, mr, mv);
					if (0 == ((i - 1) % 6))
						System.out.printf("\n");
				}
				rleHist[mr] = -1;
			} else
				break;
		}
		rleUsed = i - 1;

		if ((flags & F_STATS) != 0)
			if (((i - 1) % 6) != 1)
				System.out.printf("\n");
		InitRleLen();
	}

	private int PackLz77(int lzsz, int flags, int endAddr, int memEnd, int type) {
		int i, j, p;
		int escape = 0;
		// #ifdef HASH_COMPARE
		int[] hashValue;
		int a;
		int k;
		// #endif /* HASH_COMPARE */

		int[] lastPair;
		if (lzsz < 0 || lzsz > lrange) {
			System.err.printf(
					"LZ range must be from 0 to %d (was %d). Set to %d.\n",
					lrange, lzsz, lrange);
			lzsz = lrange;
		}
		if (lzsz > 65535) {
			System.err
					.printf("LZ range must be from 0 to 65535 (was %d). Set to 65535.\n",
							lzsz);
			lzsz = 65535;
		}
		if (0 == lzsz)
			System.err
					.printf("Warning: zero LZ range. Only RLE packing used.\n");

		InitRleLen();
		length = new int[inlen + 1];
		mode = new int[inlen];
		rle = new int[inlen];
		elr = new int[inlen];
		lzlen = new int[inlen];
		lzpos = new int[inlen];
		lzmlen = new int[inlen];
		lzmpos = new int[inlen];
		if ((type & FIXF_DLZ) != 0) {
			lzlen2 = new int[inlen];
			lzpos2 = new int[inlen];
		} else {
			lzlen2 = lzpos2 = null;
		}
		newesc = new byte[inlen];
		backSkip = new int[inlen];
		hashValue = new int[inlen];
		lastPair = new int[256 * 256];

		i = 0;
		j = 0;
		a = inlen;
		for (p = inlen - 1; p >= 0; p--) {
			k = j;
			j = i;
			i = indata[--a] & 0xff; /* Only one read per position */

			/* Without hash: 18.56%, end+middle: 12.68% */
			/* hashValue[p] = i*2 ^ j*3 ^ k*5; *//* 8.56% */
			/* hashValue[p] = i ^ j*2 ^ k*3; *//* 8.85% */
			/* hashValue[p] = i + j + k; *//* 9.33% */
			/* hashValue[p] = i + j*2 + k*3; *//* 8.25% */
			/* hashValue[p] = i*2 + j*3 + k*5; *//* 8.29% */
			/* hashValue[p] = i*3 + j*5 + k*7; *//* 7.95% */
			hashValue[p] = i * 3 + j * 5 + k * 7; /* 7.95 % */
		}
		/* Detect all RLE and LZ77 jump possibilities */
		for (p = 0; p < inlen; p++) {
			/* check run-length code - must be done, LZ77 search needs it! */
			if (rle[p] <= 0) {
				/*
				 * There are so few RLE's and especially so few long RLE's that
				 * byte-by-byte is good enough.
				 */
				a = p;
				int val = indata[a++] & 0xff; /*
											 * if this were uchar, it would go
											 * to stack..
											 */
				int top = inlen - p;
				int rlelen = 1;

				/* Loop for the whole RLE */
				while (rlelen < top && (indata[a++] & 0xff) == val) {
					rlelen++;
				}

				if (rlelen >= 2) {
					rleHist[indata[p] & 0xff]++;

					for (i = rlelen - 1; i >= 0; i--) {
						rle[p + i] = rlelen - i;
						elr[p + i] = i; /* For RLE backward skipping */
					}
				}
			}

			/* check LZ77 code */
			if (p + rle[p] + 1 < inlen) {
				int bot = p - lzsz, maxval, maxpos, rlep = rle[p];
				int hashCompare, valueCompare = 0;
				hashCompare = hashValue[p];

				/*
				 * There's always 1 equal byte, although it may not be marked as
				 * RLE.
				 */
				if (rlep <= 0)
					rlep = 1;
				if (bot < 0)
					bot = 0;
				bot += (rlep - 1);

				/*
				 * First get the shortest possible match (if any). If there is
				 * no 2-byte match, don't look further, because there can't be a
				 * longer match.
				 */
				i = lastPair[((indata[p] & 0xff) << 8) | (indata[p + 1] & 0xff)] - 1;
				if (i >= 0 && i >= bot) {
					/* Got a 2-byte match at least */
					maxval = 2;
					maxpos = p - i;

					/*
					 * A..AB rlep # of A's, B is something else..
					 * 
					 * Search for bytes that are in p + (rlep-1), i.e. the last
					 * rle byte ('A') and the non-matching one ('B'). When
					 * found, check if the rle in the compare position (i) is
					 * long enough (i.e. the same number of A's at p and
					 * i-rlep+1).
					 * 
					 * There are dramatically less matches for AB than for AA,
					 * so we get a huge speedup with this approach. We are still
					 * guaranteed to find the most recent longest match there
					 * is.
					 */

					i = lastPair[((indata[p + (rlep - 1)] & 0xff) << 8)
							| (indata[p + rlep] & 0xff)] - 1;
					while (i >= bot /* && i>=rlep-1 */) { /*
														 * bot>=rlep-1, i>=bot
														 * ==> i>=rlep-1
														 */

						/* Equal number of A's ? */
						if (0 == (rlep - 1) || rle[i - (rlep - 1)] == rlep) { /*
																			 * 'head'
																			 * matches
																			 */
							/*
							 * Check the hash values corresponding to the last
							 * two bytes of the currently longest match and the
							 * first new matching(?) byte. If the hash values
							 * don't match, don't bother to check the data
							 * itself.
							 */
							if ((hashValue[i + maxval - rlep - 1] == hashCompare)
									|| ((indata[i + maxval - rlep + 1] & 0xff) == valueCompare) /* HASH_COMPARE */
							) {
								a = i + 2; /* match */
								int b = p + rlep - 1 + 2;/* curpos */
								int topindex = inlen - (p + rlep - 1);

								/* the 2 first bytes ARE the same.. */
								j = 2;
								while (j < topindex
										&& indata[a++] == indata[b++])
									j++;

								if (j + rlep - 1 > maxval) {
									int tmplen = j + rlep - 1, tmppos = p - i
											+ rlep - 1;

									if (tmplen > maxlzlen)
										tmplen = maxlzlen;

									if (lzmlen[p] < tmplen) {
										lzmlen[p] = tmplen;
										lzmpos[p] = tmppos;
									}
									/*
									 * Accept only versions that really are
									 * shorter
									 */
									if (tmplen * 8 - LenLz(tmplen, tmppos) > maxval
											* 8 - LenLz(maxval, maxpos)) {
										maxval = tmplen;
										maxpos = tmppos;
										hashCompare = hashValue[p + maxval - 2];
									}
									if (maxval == maxlzlen)
										break;
								}
							}
						}
						if (0 == backSkip[i])
							break; /* No previous occurrances (near enough) */
						i -= backSkip[i];
					}

					/*
					 * If there is 'A' in the previous position also, RLE-like
					 * LZ77 is possible, although rarely shorter than real RLE.
					 */
					if (p != 0 && rle[p - 1] > maxval) {
						maxval = rle[p - 1] - 1;
						maxpos = 1;
					}
					/*
					 * Last, try to find as long as possible match for the RLE
					 * part only.
					 */
					if (maxval < maxlzlen && rlep > maxval) {
						bot = p - lzsz;
						if (bot < 0)
							bot = 0;

						/* Note: indata[p] == indata[p+1] */
						i = lastPair[(indata[p] & 0xff) * 257] - 1;
						while (/* i>= rlep-2 && */i >= bot) {
							if (elr[i] + 2 > maxval) {
								maxval = Math.min(elr[i] + 2, rlep);
								maxpos = p - i + (maxval - 2);
								if (maxval == rlep)
									break; /* Got enough */
							}
							i -= elr[i];
							if (0 == backSkip[i])
								break; /*
										 * No previous occurrances (near enough)
										 */
							i -= backSkip[i];
						}
					}
					if (p + maxval > inlen) {
						System.err
								.printf("Error @ %d, lzlen %d, pos %d - exceeds inlen\n",
										p, maxval, maxpos);
						maxval = inlen - p;
					}
					if (lzmlen[p] < maxval) {
						lzmlen[p] = maxval;
						lzmpos[p] = maxpos;
					}
					if (maxpos <= 256 || maxval > 2) {
						if (maxpos < 0)
							System.err.printf("Error @ %d, lzlen %d, pos %d\n",
									p, maxval, maxpos);
						lzlen[p] = (maxval < maxlzlen) ? maxval : maxlzlen;
						lzpos[p] = maxpos;
					}
				}
			}
			/* check LZ77 code again, ROT1..255 */
			if ((type & FIXF_DLZ) != 0
					&& /* rle[p]<maxlzlen && */p + rle[p] + 1 < inlen) {
				int rot;

				for (rot = 1; rot < 255/* BUG:?should be 256? */; rot++) {
					int bot = p - /* lzsz */256, maxval, maxpos, rlep = rle[p];
					int valueCompare = ((indata[p + 2] & 0xff) + rot) & 0xff;

					/*
					 * There's always 1 equal byte, although it may not be
					 * marked as RLE.
					 */
					if (rlep <= 0)
						rlep = 1;
					if (bot < 0)
						bot = 0;
					bot += (rlep - 1);

					/*
					 * First get the shortest possible match (if any). If there
					 * is no 2-byte match, don't look further, because there
					 * can't be a longer match.
					 */
					i = lastPair[((((indata[p] & 0xff) + rot) & 0xff) << 8)
							| (((indata[p + 1] & 0xff) + rot) & 0xff)] - 1;
					if (i >= 0 && i >= bot) {
						/* Got a 2-byte match at least */
						maxval = 2;
						maxpos = p - i;

						/*
						 * A..AB rlep # of A's, B is something else..
						 * 
						 * Search for bytes that are in p + (rlep-1), i.e. the
						 * last rle byte ('A') and the non-matching one ('B').
						 * When found, check if the rle in the compare position
						 * (i) is long enough (i.e. the same number of A's at p
						 * and i-rlep+1).
						 * 
						 * There are dramatically less matches for AB than for
						 * AA, so we get a huge speedup with this approach. We
						 * are still guaranteed to find the most recent longest
						 * match there is.
						 */

						i = lastPair[((((indata[p + (rlep - 1)] & 0xff) + rot) & 0xff) << 8)
								| (((indata[p + rlep] & 0xff) + rot) & 0xff)] - 1;
						while (i >= bot /* && i>=rlep-1 */) { /*
															 * bot>=rlep-1,
															 * i>=bot ==>
															 * i>=rlep-1
															 */

							/* Equal number of A's ? */
							if (0 == (rlep - 1) || rle[i - (rlep - 1)] == rlep) { /*
																				 * 'head'
																				 * matches
																				 */
								/*
								 * Check the hash values corresponding to the
								 * last two bytes of the currently longest match
								 * and the first new matching(?) byte. If the
								 * hash values don't match, don't bother to
								 * check the data itself.
								 */
								if ((indata[i + maxval - rlep + 1] & 0xff) == valueCompare) {
									a = i + 2; /* match */
									int b = p + rlep - 1 + 2;/* curpos */
									int topindex = inlen - (p + rlep - 1);

									/* the 2 first bytes ARE the same.. */
									j = 2;
									while (j < topindex
											&& (indata[a++] & 0xff) == (((indata[b++] & 0xff) + rot) & 0xff))
										j++;

									if (j + rlep - 1 > maxval) {
										int tmplen = j + rlep - 1, tmppos = p
												- i + rlep - 1;

										if (tmplen > maxlzlen)
											tmplen = maxlzlen;

										/*
										 * Accept only versions that really are
										 * shorter
										 */
										if (tmplen * 8 - LenLz(tmplen, tmppos) > maxval
												* 8 - LenLz(maxval, maxpos)) {
											maxval = tmplen;
											maxpos = tmppos;

											valueCompare = ((indata[p + maxval] & 0xff) + rot) & 0xff;
										}
										if (maxval == maxlzlen)
											break;
									}
								}
							}
							if (0 == backSkip[i])
								break; /*
										 * No previous occurrances (near enough)
										 */
							i -= backSkip[i];
						}

						if (p + maxval > inlen) {
							System.err
									.printf("Error @ %d, lzlen %d, pos %d - exceeds inlen\n",
											p, maxval, maxpos);
							maxval = inlen - p;
						}
						if (maxval > 3
								&& maxpos <= 256
								&& (maxval > lzlen2[p] || (maxval == lzlen2[p] && maxpos < lzpos2[p]))) {
							if (maxpos < 0)
								System.err.printf(
										"Error @ %d, lzlen %d, pos %d\n", p,
										maxval, maxpos);
							lzlen2[p] = (maxval < maxlzlen) ? maxval : maxlzlen;
							lzpos2[p] = maxpos;
						}
					}
				}
				if (lzlen2[p] <= lzlen[p] || lzlen2[p] <= rle[p]) {
					lzlen2[p] = lzpos2[p] = 0;
				}
			}

			/*
			 * Update the two-byte history ('hash table') & backSkip ('linked
			 * list')
			 */
			if (p + 1 < inlen) {
				int index = ((indata[p] & 0xff) << 8) | (indata[p + 1] & 0xff);
				int ptr = p - (lastPair[index] - 1);

				if (ptr > p || ptr > 0xffff)
					ptr = 0;

				backSkip[p] = ptr;
				lastPair[index] = p + 1;
			}
		}
		if ((flags & F_NORLE) != 0) {
			for (p = 1; p < inlen; p++) {
				if (rle[p - 1] - 1 > lzlen[p]) {
					lzlen[p] = (rle[p] < maxlzlen) ? rle[p] : maxlzlen;
					lzpos[p] = 1;
				}
			}
			for (p = 0; p < inlen; p++) {
				rle[p] = 0;
			}
		}
		System.out.printf("\rChecked: %d \n", p);

		/* Initialize the RLE selections */
		InitRle(flags);

		/* Check the normal bytes / all ratio */
		if ((flags & F_AUTO) != 0) {
			int mb, mv;

			System.out.printf("Selecting the number of escape bits.. ");

			/*
			 * Absolute maximum number of escaped bytes with the escape optimize
			 * is 2^-n, where n is the number of escape bits used.
			 * 
			 * This worst case happens only on equal- distributed normal bytes
			 * (01230123..). This is why the typical values are so much smaller.
			 */

			mb = 0;
			mv = 8 * OUT_SIZE;
			for (escBits = 1; escBits < 9; escBits++) {
				int escaped, other = 0, c;

				escMask = (0xff00 >> escBits) & 0xff;

				/* Find the optimum path for selected escape bits (no optimize) */
				OptimizeLength(0);

				/* Optimize the escape selections for this path & escBits */
				IntContainer escapeCont = new IntContainer(escape);
				IntContainer otherCont = new IntContainer(other);
				escaped = OptimizeEscape(escapeCont, otherCont);
				escape = escapeCont.intVal;
				other = otherCont.intVal;

				/* Compare value: bits lost for escaping -- bits lost for prefix */
				c = (escBits + 3) * escaped + other * escBits;
				if ((flags & F_STATS) != 0) {
					System.out.printf(" %d:%d", escBits, c);
				}
				if (c < mv) {
					mb = escBits;
					mv = c;
				} else {
					/* minimum found */
					break;
				}
				if (escBits == 4 && (flags & F_STATS) != 0)
					System.out.printf("\n");
			}
			if (mb == 1) { /* Minimum was 1, check 0 */
				int escaped;

				escBits = 0;
				escMask = 0;

				/* Find the optimum path for selected escape bits (no optimize) */
				OptimizeLength(0);
				/* Optimize the escape selections for this path & escBits */
				IntContainer escapeCont = new IntContainer(escape);
				escaped = OptimizeEscape(escapeCont, null);
				escape = escapeCont.intVal;

				if ((flags & F_STATS) != 0) {
					System.out.printf(" %d:%d", escBits, 3 * escaped);
				}
				if (3 * escaped < mv) {
					mb = 0;
					/* mv = 3*escaped; */
				}
			}
			if ((flags & F_STATS) != 0)
				System.out.printf("\n");

			System.out.printf("Selected %d-bit escapes\n", mb);
			escBits = mb;
			escMask = (0xff00 >> escBits) & 0xff;
		}

		if (0 == (flags & F_NOOPT)) {
			System.out.printf("Optimizing LZ77 and RLE lengths...");
		}

		/* Find the optimum path (optimize) */
		OptimizeLength((flags & F_NOOPT) != 0 ? 0 : 1);
		if ((flags & F_STATS) != 0) {
			if (0 == (flags & F_NOOPT))
				System.out.printf(" gained %d units.\n", lzopt / 8);
		} else
			System.out.printf("\n");

		if ((flags & F_AUTOEX) != 0) {
			int lzstat[] = new int[] { 0, 0, 0, 0, 0 }, cur = 0, old = extraLZPosBits;

			System.out.printf("Selecting LZPOS LO length.. ");

			for (p = 0; p < inlen;) {
				switch (mode[p]) {
				case LZ77: /* lz */
					extraLZPosBits = 0;
					lzstat[0] += LenLz(lzlen[p], lzpos[p]);
					extraLZPosBits = 1;
					lzstat[1] += LenLz(lzlen[p], lzpos[p]);
					extraLZPosBits = 2;
					lzstat[2] += LenLz(lzlen[p], lzpos[p]);
					extraLZPosBits = 3;
					lzstat[3] += LenLz(lzlen[p], lzpos[p]);
					extraLZPosBits = 4;
					lzstat[4] += LenLz(lzlen[p], lzpos[p]);
					p += lzlen[p];
					break;
				// #ifdef DELTA
				case DLZ:
					p += lzlen2[p];
					break;
				// #endif
				case RLE: /* rle */
					p += rle[p];
					break;

				default: /* normal */
					p++;
					break;
				}
			}
			for (i = 0; i < 5; i++) {
				if ((flags & F_STATS) != 0)
					System.out.printf(" %d:%d", i + 8, lzstat[i]);

				/* first time around (lzstat[0] < lzstat[0]) */
				if (lzstat[i] < lzstat[cur])
					cur = i;
			}
			extraLZPosBits = (flags & F_AUTOEX) != 0 ? cur : old;

			if ((flags & F_STATS) != 0)
				System.out.printf("\n");

			System.out.printf("Selected %d-bit LZPOS LO part\n",
					extraLZPosBits + 8);
			if (cur != old) {
				System.out
						.printf("Note: Using option -p%d you may get better results.\n",
								cur);
			}
			/* Find the optimum path (optimize) */
			if (extraLZPosBits != old)
				OptimizeLength((flags & F_NOOPT) != 0 ? 0 : 1);
		}
		if (true) {
			int stat[] = new int[] { 0, 0, 0, 0 };

			for (p = 0; p < inlen;) {
				switch (mode[p]) {
				case LZ77: /* lz */
					if ((lzpos[p] >> 8) + 1 > (1 << maxGamma))
						stat[3]++;
					if (lzlen[p] > (1 << maxGamma))
						stat[0]++;
					p += lzlen[p];
					break;

				case RLE: /* rle */
					if (rle[p] > (1 << (maxGamma - 1))) {
						if (rle[p] <= (1 << maxGamma))
							stat[1]++;
					}
					p += rle[p];
					break;
				// #ifdef DELTA
				case DLZ:
					p += lzlen2[p];
					break;
				// #endif
				default: /* normal */
					p++;
					break;
				}
			}
			/* TODO: better formula.. */
			if (maxGamma < 7 && stat[0] + stat[1] + stat[3] > 10) {
				System.out
						.printf("Note: Using option -m%d you may get better results.\n",
								maxGamma + 1);
			}
			if (maxGamma > 5 && stat[0] + stat[1] + stat[3] < 4) {
				System.out
						.printf("Note: Using option -m%d you may get better results.\n",
								maxGamma - 1);
			}
		}

		/* Optimize the escape selections */
		IntContainer escapeCont = new IntContainer(escape);
		OptimizeEscape(escapeCont, null);
		escape = escapeCont.intVal;
		IntContainer startEscape = new IntContainer(escape);
		startEscape.intVal = escape;
		OptimizeRle(flags); /* Retune the RLE selections */

		/* Perform rescan */
		{
			int esc = escape;

			for (p = 0; p < inlen;) {
				switch (mode[p]) {
				case LITERAL: /* normal */
					if ((indata[p] & escMask) == esc) {
						esc = newesc[p] & 0xff;
					}
					p++;
					break;

				// #ifdef DELTA
				case DLZ:
					p += lzlen2[p];
					break;
				// #endif

				case LZ77: /* lz77 */

					/* Re-search matches to get the closest one */
					if (lzopt != 0 && /* If any changes to lengths.. */
					lzlen[p] > 2 /* && lzlen[p] > rle[p] */) {
						int bot = p - lzpos[p] + 1;
						int rlep = rle[p];

						if (0 == rlep)
							rlep = 1;
						if (bot < 0)
							bot = 0;
						bot += (rlep - 1);

						i = p - backSkip[p];
						while (i >= bot /* && i>=rlep-1 */) {
							/* Equal number of A's ? */
							if (rlep == 1 || rle[i - rlep + 1] == rlep) { /*
																		 * 'head'
																		 * matches
																		 */
								a = i + 1; /* match */
								int b = p + rlep - 1 + 1; /* curpos */
								int topindex = inlen - (p + rlep - 1);

								j = 1;
								while (j < topindex
										&& indata[a++] == indata[b++])
									j++;

								if (j + rlep - 1 >= lzlen[p]) {
									int tmppos = p - i + rlep - 1;

									lzpos[p] = tmppos;
									break;
								}
							}
							if (0 == backSkip[i])
								break; /*
										 * No previous occurrances (near enough)
										 */
							i -= backSkip[i];
						}
					}

					p += lzlen[p];
					break;

				case RLE: /* rle */
					p += rle[p];
					break;

				default: /* Error Flynn :-) */
					System.err.printf("Internal error: mode %d\n", mode[p]);
					p++;
					break;
				}
			}
		}

		/* start of output */

		for (p = 0; p < inlen;) {
			switch (mode[p]) {
			case LITERAL: /* normal */
				length[p] = outPointer;

				escapeCont = new IntContainer(escape);
				OutputNormal(escapeCont, indata, p, newesc[p] & 0xff);
				escape = escapeCont.intVal;
				p++;
				break;

			// #ifdef DELTA
			case DLZ:
				for (i = 0; i < lzlen2[p]; i++)
					length[p + i] = outPointer;
				escapeCont = new IntContainer(escape);
				OutputDLz(
						escapeCont,
						lzlen2[p],
						lzpos2[p],
						((indata[p] & 0xff) - (indata[p - lzpos2[p]] & 0xff)) & 0xff);
				escape = escapeCont.intVal;
				p += lzlen2[p];
				break;
			// #endif

			case LZ77: /* lz77 */
				for (i = 0; i < lzlen[p]; i++)
					length[p + i] = outPointer;
				escapeCont = new IntContainer(escape);
				OutputLz(escapeCont, lzlen[p], lzpos[p], indata, p - lzpos[p],
						p);
				escape = escapeCont.intVal;
				p += lzlen[p];
				break;

			case RLE: /* rle */
				for (i = 0; i < rle[p]; i++)
					length[p + i] = outPointer;
				escapeCont = new IntContainer(escape);
				OutputRle(escapeCont, indata, p, rle[p]);
				escape = escapeCont.intVal;
				p += rle[p];
				break;

			default: /* Error Flynn :-) */
				System.err.printf("Internal error: mode %d\n", mode[p]);
				p++;
				break;
			}
		}
		escapeCont = new IntContainer(escape);
		OutputEof(escapeCont);
		escape = escapeCont.intVal;

		/* xxxxxxxxxxxxxxxxxxx uncompressed */
		/* yyyyyyyyyyyyyyyyy compressed */
		/* zzzz */

		i = inlen;
		for (p = 0; p < inlen; p++) {
			int pos = (inlen - outPointer) + length[p] - p;
			i = Math.min(i, pos);
		}
		if (i < 0)
			reservedBytes = -i + 2;
		else
			reservedBytes = 0;

		return startEscape.intVal;

	}

	private int getSYSAddr(int offset) {
		int execAddr = -1;
		if (0 <= offset) {
			for (int i = offset; i < offset + 60; i++) {
				if (indata[i] == (byte) 0x9e) { /* SYS token */
					execAddr = 0;
					i++;
					/* Skip spaces and parens */
					while (indata[i] == '(' || indata[i] == ' ')
						i++;

					while (indata[i] >= '0' && indata[i] <= '9') {
						execAddr = execAddr * 10 + indata[i++] - '0';
					}
					break;
				}
			}
		}
		return execAddr;
	}

	public void crunch(String input, String output) throws IOException {
		try (DataInputStream infp = new DataInputStream(new FileInputStream(
				input)); PrintStream outfp = new PrintStream(output)) {

			lrange = LRANGE;
			maxlzlen = MAXLZLEN;
			maxrlelen = MAXRLELEN;
			InitValueLen();

			int startAddr = (infp.read() & 0xff) + 256 * (infp.read() & 0xff);

			/* Read in the data */
			indata = new byte[lrange];
			int count = 0;
			while (inlen < lrange
					&& (count = infp.read(indata, inlen, lrange - inlen)) >= 0) {
				inlen += count;
			}

			if (startAddr < 0x258 || startAddr + inlen - 1 > 0xffff) {
				throw new RuntimeException(
						"Only programs from 0x0258 to 0xffff can be compressed");
			}

			int type = FIXF_C64 | FIXF_WRAP | FIXF_BASIC; /*
														 * C64, wrap active,
														 * basic
														 */
			int memStart = 0x801; /* Loading address */
			int memEnd = 0x10000;

			int execAddr = getSYSAddr(memStart - startAddr);
			if (execAddr < startAddr || execAddr >= startAddr + inlen) {
				if ((type & FIXF_BASIC) != 0) {
					execAddr = 0xa7ae;
				} else {
					throw new RuntimeException(
							"Note: The execution address was not detected correctly!");
				}
			}
			if (VERBOSE) {
				System.out.printf("Crunching " + input);
				System.out.printf("Load address 0x%04x, End address 0x%04x\n",
						startAddr, startAddr + inlen - 1);
				System.out.printf("Exec address 0x%04x\n", execAddr);
				System.out.printf("New load address 0x%04x\n", memStart);
				System.out
						.printf("Interrupts %s and memory config set to $%02x "
								+ "after decompression\n",
								(cliConfig == 0x58) ? "enabled" : "disabled",
								memConfig);
			}

			int flags = F_2MHZ | F_AUTO | F_AUTOEX;

			int startEscape = PackLz77(DEFAULT_LZLEN, flags, startAddr + inlen,
					memEnd, type);
			int endAddr = startAddr + inlen; /* end for uncompressed data */
			int progEnd = endAddr;
			int hDeCall = 512/* estimated maximum decruncher size */;
			if (endAddr - ((outPointer + 255) & ~255) < memStart + hDeCall + 3) {
				/* would overwrite the decompressor, move a bit upwards */
				System.out.printf(
						"$%x < $%x, decompressor overwrite possible, "
								+ "moving upwards\n", endAddr
								- ((outPointer + 255) & ~255), memStart
								+ hDeCall + 3);
				endAddr = memStart + hDeCall + 3 + ((outPointer + 255) & ~255);
			}
			/* 3 bytes reserved for EOF */
			/* bytes reserved for temporary data expansion (escaped chars) */
			endAddr += 3 + reservedBytes;

			if (0 == timesDLz) {
				type &= ~FIXF_DLZ;
			}
			SavePack(type, outBuffer, outPointer, outfp, startAddr, execAddr,
					startEscape, rleValues, endAddr, progEnd, extraLZPosBits,
					(flags & F_2MHZ) != 0, memStart, memEnd);

			System.out.printf("Compressed %d bytes\n", inlen);
		}
	}

}
