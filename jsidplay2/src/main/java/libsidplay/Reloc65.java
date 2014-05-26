package libsidplay;

import java.nio.ByteBuffer;
import java.util.Map;

public class Reloc65 {

	private int tbase, tdiff, tlen, dlen;

	private String[] globalStrings;

	private byte[] buf, segt, segd, utab, rttab, rdtab, extab;

	private Map<String, Integer> globals;

	private static final int HEADER_LEN = (9 * 2 + 8);

	private final byte HEADER[] = { 1, 0, 'o', '6', '5' };

	public ByteBuffer reloc65(final byte[] data, final int addr,
			final Map<String, Integer> globals) {
		this.buf = data;
		this.globals = globals;

		for (int i = 0; i < 5; i++) {
			if (buf[i] != HEADER[i]) {
				throw new RuntimeException("Reloc65: Invalid header!");
			}
		}
		int mode = readWord(buf, 6);
		if ((mode & 0x2000) != 0 || (mode & 0x4000) != 0) {
			throw new RuntimeException("Reloc65: Invalid mode in header!");
		}

		tbase = readWord(buf, 8);
		tdiff = addr - tbase;
		tlen = readWord(buf, 10);
		dlen = readWord(buf, 14);

		int headerLength = HEADER_LEN + readOptions(buf);

		segt = buf;
		int segtPos = headerLength;
		segd = segt;
		int sedPos = segtPos + tlen;
		utab = segd;
		int utabPos = sedPos + dlen;

		rttab = utab;
		int rttabPos = utabPos + readUndef(utab, utabPos);

		rdtab = rttab;
		extab = rdtab;
		int rdtabPos = relocSegment(segt, segtPos, tlen, rttab, rttabPos);
		int extabPos = relocSegment(segd, sedPos, dlen, rdtab, rdtabPos);

		relocGlobals(extab, extabPos);

		writeWord(buf, 8, addr);
		return ByteBuffer.wrap(segt, segtPos, tlen);
	}

	private void writeWord(final byte[] buf, final int pos, final int value) {
		buf[pos] = (byte) (value & 255);
		buf[pos + 1] = (byte) (value >> 8);
	}

	private int readWord(final byte[] buf, final int pos) {
		return ((buf[pos + 1] & 0xff) << 8) + (buf[pos] & 0xff);
	}

	private int readOptions(byte[] buf) {
		int l = 0;
		int c = buf[HEADER_LEN] & 0xff;
		while ((c != 0)) {
			l += c;
			c = buf[HEADER_LEN + l] & 0xff;
		}
		return ++l;
	}

	private int readUndef(byte[] buf, int pos) {
		globalStrings = new String[readWord(buf, pos)];
		int endPos = 2;
		int currentString = 0;
		while (currentString < globalStrings.length) {
			StringBuffer str = new StringBuffer();
			for (int i = 0; i < buf.length; i++) {
				final byte ch = buf[pos + endPos + i];
				if (ch == 0) {
					globalStrings[currentString] = str.toString();
					break;
				}
				str.append((char) ch);
			}
			endPos += str.length() + 1;
			currentString++;
		}
		return endPos;
	}

	private int relocSegment(byte[] buf, int bufPos, int len, byte[] rtab,
			int rtabPos) {
		int adr = -1;
		int seg, old;
		while (rtab[rtabPos] != 0) {
			if (rtab[rtabPos] == -1) {
				adr += 254;
				rtabPos++;
			} else {
				adr += rtab[rtabPos++] & 255;
				int type = rtab[rtabPos] & 0xe0;
				seg = rtab[rtabPos++] & 0x07;
				switch (type) {
				case 0x80:
					old = readWord(buf, bufPos + adr);
					if (seg != 0) {
						writeWord(buf, bufPos + adr, old + reldiff(seg));
					} else {
						writeWord(buf, bufPos + adr,
								old + findGlobal(rtab, rtabPos));
					}
					break;
				case 0x40:
					old = (buf[bufPos + adr] & 0xff) << 8 + (rtab[rtabPos] & 0xff);
					if (seg != 0) {
						int newValue = old + reldiff(seg);
						buf[bufPos + adr] = (byte) (newValue >> 8);
						rtab[rtabPos++] = (byte) (newValue & 255);
					} else {
						int newValue = old + findGlobal(rtab, rtabPos);
						buf[bufPos + adr] = (byte) (newValue >> 8);
						rtab[rtabPos++] = (byte) (newValue & 255);
					}
					break;
				case 0x20:
					old = buf[bufPos + adr] & 0xff;
					if (seg != 0) {
						buf[bufPos + adr] = (byte) ((old + reldiff(seg)) & 255);
					} else {
						buf[bufPos + adr] = (byte) ((old + findGlobal(rtab,
								rtabPos)) & 255);
					}
					break;
				}
				if (seg == 0) {
					rtabPos += 2;
				}
			}
		}
		if (adr > len) {
			throw new RuntimeException(
					"Reloc65: Warning: relocation table entries past segment end!");
		}
		return ++rtabPos;
	}

	private int findGlobal(byte[] bp, int bpPos) {
		final String name = globalStrings[readWord(bp, bpPos)];
		return globals.get(name);
	}

	private int relocGlobals(final byte[] buf, int bufPos) {
		int n = readWord(buf, bufPos);
		bufPos += 2;

		while (n != 0) {
			while (buf[bufPos++] != 0) {
			}
			int seg = buf[bufPos] & 0xff;
			int old = readWord(buf, bufPos + 1);
			if (seg != 0) {
				writeWord(buf, bufPos + 1, old + reldiff(seg));
			} else {
				writeWord(buf, bufPos + 1, old + findGlobal(buf, bufPos + 1));
			}
			bufPos += 3;
			n--;
		}
		return bufPos;
	}

	private int reldiff(int s) {
		return s == 2 ? tdiff : 0;
	}

}
