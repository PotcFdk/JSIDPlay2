package libsidplay;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import libpsid64.NotEnoughC64MemException;
import libpsid64.Psid64;
import libsidplay.sidtune.SidTuneError;
import sidplay.ini.IniConfig;

public class Reloc65 {

	private byte[] buf;

	private int tbase, tlen, dlen;

	private int tdiff;

	private String[] ud;

	private byte[] segt;

	private byte[] segd;

	private byte[] utab;

	private byte[] rttab;

	private byte[] rdtab;

	private byte[] extab;

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
			throw new RuntimeException(
					"Reloc65: Invalid mode in header, expected 0!");
		}

		tbase = readWord(buf, 8);
		tlen = readWord(buf, 10);
		tdiff = addr - tbase;
		dlen = readWord(buf, 14);

		int headerLength = HEADER_LEN + readOptions(buf, HEADER_LEN);

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

	private int readOptions(byte[] buf, int pos) {
		int l = 0;
		int c = buf[pos + 0] & 0xff;
		while ((c != 0)) {
			l += c;
			c = buf[pos + l] & 0xff;
		}
		return ++l;
	}

	private int readUndef(byte[] buf, int pos) {
		int n = readWord(buf, pos);
		ud = new String[n];
		int endPos = 2;
		int i = 0;
		while (i < n) {
			StringBuffer str = new StringBuffer();
			for (int j = 0; j < buf.length; j++) {
				if (buf[pos + endPos + j] == 0) {
					ud[i] = str.toString();
					break;
				}
				str.append((char) buf[pos + endPos + j]);
			}
			endPos += str.length() + 1;
			i++;
		}
		return endPos;
	}

	private int relocSegment(byte[] buf, int bufPos, int len, byte[] rtab,
			int rtabPos) {
		int adr = -1;
		int type, seg, old, newv;
		while (rtab[rtabPos] != 0) {
			if ((rtab[rtabPos] & 255) == 255) {
				adr += 254;
				rtabPos++;
			} else {
				adr += rtab[rtabPos] & 255;
				rtabPos++;
				type = rtab[rtabPos] & 0xe0;
				seg = rtab[rtabPos] & 0x07;
				rtabPos++;
				switch (type) {
				case 0x80:
					old = (buf[bufPos + adr] & 0xff) + 256
							* (buf[bufPos + adr + 1] & 0xff);
					if (seg != 0)
						newv = old + reldiff(seg);
					else
						newv = old + findGlobal(rtab, rtabPos);
					buf[bufPos + adr] = (byte) (newv & 255);
					buf[bufPos + adr + 1] = (byte) ((newv >> 8) & 255);
					break;
				case 0x40:
					old = (buf[bufPos + adr] & 0xff) * 256
							+ (rtab[rtabPos] & 0xff);
					if (seg != 0)
						newv = old + reldiff(seg);
					else
						newv = old + findGlobal(rtab, rtabPos);
					buf[bufPos + adr] = (byte) ((newv >> 8) & 255);
					rtab[rtabPos] = (byte) (newv & 255);
					rtabPos++;
					break;
				case 0x20:
					old = buf[bufPos + adr] & 0xff;
					if (seg != 0)
						newv = old + reldiff(seg);
					else
						newv = old + findGlobal(rtab, rtabPos);
					buf[bufPos + adr] = (byte) (newv & 255);
					break;
				}
				if (seg == 0)
					rtabPos += 2;
			}
		}
		if (adr > len) {
			throw new RuntimeException(
					"Reloc65: Warning: relocation table entries past segment end!");
		}
		return ++rtabPos;
	}

	private int findGlobal(byte[] bp, int bpPos) {
		int nl = (bp[bpPos + 0] & 0xff) + 256 * (bp[bpPos + 1] & 0xff);
		String name = ud[nl];
		return globals.get(name);
	}

	private int relocGlobals(final byte[] buf, int bufPos) {
		int n = readWord(buf, bufPos);
		bufPos += 2;

		while (n != 0) {
			while ((buf[bufPos++]) != 0) {
			}
			int seg = buf[bufPos] & 0xff;
			int old = readWord(buf, bufPos + 1);
			int newValue;
			if (seg != 0) {
				newValue = old + reldiff(seg);
			} else {
				newValue = old + findGlobal(buf, bufPos + 1);
			}
			writeWord(buf, bufPos + 1, newValue);
			bufPos += 3;
			n--;
		}
		return bufPos;
	}

	private int reldiff(int s) {
		return s == 2 ? tdiff : 0;
	}

	public static void main(String[] args) throws NotEnoughC64MemException,
			IOException, SidTuneError {
		Psid64 p = new Psid64();
		p.setTmpDir("D:/");
		p.convertFiles(
				new Player(new IniConfig()),
				new File[] { new File(
						"d:/workspace/jsidplay2/src/test/resources/pucrunch/Turrican_2-The_Final_Fight.sid") },
				new File("d:/"));
	}
}
