package libsidutils.reloc65;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

public class Reloc65 {

	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	private static final byte CMP_O64[] = { 1, 0, 'o', '6', '5' };

	private static final int bufPos = (9 * 2 + 8); /* 16 bit header */

	private int tdiff;

	private String[] ud;

	private HashMap<String, Integer> globals;

	public ByteBuffer reloc65(byte[] buf, int addr) {
		if (!ByteBuffer.wrap(buf, 0, CMP_O64.length).equals(ByteBuffer.wrap(CMP_O64))) {
			return null;
		}
		int hlen = bufPos + read_options(buf, bufPos);
		int tbase = ((buf[9] & 0xff) << 8) + (buf[8] & 0xff);
		int tlen = ((buf[11] & 0xff) << 8) + (buf[10] & 0xff);
		int dlen = ((buf[15] & 0xff) << 8) + (buf[14] & 0xff);

		tdiff = addr - tbase;
		globals = new HashMap<String, Integer>();

		byte[] segt = buf;
		int segtPos = hlen;
		byte[] segd = segt;
		int sedPos = segtPos + tlen;
		byte[] utab = segd;
		int utabPos = sedPos + dlen;

		byte[] rttab = utab;
		int rttabPos = utabPos + read_undef(utab, utabPos);

		byte[] rdtab = rttab;
		byte[] extab = rdtab;
		int rdtabPos = reloc_seg(segt, segtPos, tlen, rttab, rttabPos);
		int extabPos = reloc_seg(segd, sedPos, dlen, rdtab, rdtabPos);

		reloc_globals(extab, extabPos);

		buf[9] = (byte) ((addr >> 8) & 255);
		buf[8] = (byte) (addr & 255);

		return ByteBuffer.wrap(segt, segtPos, tlen);
	}

	private int read_options(byte[] buf, int pos) {
		int l = 0;
		int c = buf[pos + 0] & 0xff;
		while ((c != 0)) {
			l += c;
			c = buf[pos + l] & 0xff;
		}
		return ++l;
	}

	private int read_undef(byte[] buf, int pos) {
		int l = 2;
		int n = (buf[pos] & 0xff) + ((buf[pos + 1] & 0xff) << 8);
		ud = new String[n];
		int i = 0;
		while (i < n) {
			byte[] tmp = new byte[32];
			for (int j = 0; j < buf.length; j++) {
				if (buf[pos + l + j] == 0) {
					ud[i] = new String(tmp, 0, j, US_ASCII);
					break;
				}
				tmp[j] = buf[pos + l + j];
			}

			while (buf[pos + (l++)] != 0) {
			}
			i++;
		}
		return l;
	}

	private int reloc_seg(byte[] buf, int bufPos, int len, byte[] rtab, int rtabPos) {
		int adr = -1;
		while (rtab[rtabPos] != 0) {
			if ((rtab[rtabPos] & 255) == 255) {
				adr += 254;
				rtabPos++;
			} else {
				adr += rtab[rtabPos] & 255;
				rtabPos++;
				int type = rtab[rtabPos] & 0xe0;
				int seg = rtab[rtabPos] & 0x07;
				rtabPos++;
				switch (type) {
				case 0x80: {
					int old = (buf[bufPos + adr] & 0xff) + ((buf[bufPos + adr + 1] & 0xff) << 8);
					int newv;
					if (seg != 0) {
						newv = old + reldiff(seg);
					} else {
						newv = old + find_global(rtab, rtabPos);
					}
					buf[bufPos + adr] = (byte) (newv & 255);
					buf[bufPos + adr + 1] = (byte) ((newv >> 8) & 255);
					break;
				}
				case 0x40: {
					int old = ((buf[bufPos + adr] & 0xff) << 8) + (rtab[rtabPos] & 0xff);
					int newv;
					if (seg != 0) {
						newv = old + reldiff(seg);
					} else {
						newv = old + find_global(rtab, rtabPos);
					}
					buf[bufPos + adr] = (byte) ((newv >> 8) & 255);
					rtab[rtabPos] = (byte) (newv & 255);
					rtabPos++;
					break;
				}
				case 0x20: {
					int old = buf[bufPos + adr] & 0xff;
					int newv;
					if (seg != 0) {
						newv = old + reldiff(seg);
					} else {
						newv = old + find_global(rtab, rtabPos);
					}
					buf[bufPos + adr] = (byte) (newv & 255);
					break;
				}
				}
				if (seg == 0)
					rtabPos += 2;
			}
		}
		if (adr > len) {
			System.err.println("reloc65: Warning: relocation table entries past segment end!\n");
		}
		return ++rtabPos;
	}

	private int find_global(byte[] bp, int bpPos) {
		String name = ud[(bp[bpPos + 0] & 0xff) + ((bp[bpPos + 1] & 0xff) << 8)];
		return globals.get(name);
	}

	private int reloc_globals(byte[] buf, int bufPos) {
		int n = (buf[bufPos + 0] & 0xff) + ((buf[bufPos + 1] & 0xff) << 8);
		bufPos += 2;

		int newv = 0;
		while (n != 0) {
			while ((buf[bufPos++]) != 0) {
			}
			int seg = buf[bufPos] & 0xff;
			int old = (buf[bufPos + 1] & 0xff) + ((buf[bufPos + 2] & 0xff) << 8);
			if (seg != 0) {
				newv = old + reldiff(seg);
			} else {
				newv = old + find_global(buf, bufPos + 1);
			}
			buf[bufPos + 1] = (byte) (newv & 255);
			buf[bufPos + 2] = (byte) ((newv >> 8) & 255);
			bufPos += 3;
			n--;
		}
		return bufPos;
	}

	private int reldiff(int s) {
		return s == 2 ? tdiff : 0;
	}

}
