package libsidplay;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class Reloc65 {

	static class File65 {
		byte[] buf;

		int tbase, tlen, dbase, dlen, bbase, zbase;

		int tdiff, ddiff, bdiff, zdiff;

		public String []ud;

		byte[] segt;

		byte[] segd;

		byte[] utab;

		byte[] rttab;

		byte[] rdtab;

		byte[] extab;

		HashMap<String, Integer> globals;

	}
	
	private static final int BUF = (9 * 2 + 8); /* 16 bit header */

	private final File65 file = new File65();

	private final char /* unsigned char */cmp[] = {
			1, 0, 'o', '6', '5' };

	public ByteBuffer reloc65(byte[] buf,
			int fsize, int addr, HashMap<String,Integer> globals) {
		int mode, hlen;

		boolean tflag = false, dflag = false, bflag = false, zflag = false;
		int tbase = 0, dbase = 0, bbase = 0, zbase = 0;
		int extract = 0;

		file.globals = globals;

		file.buf = buf;
		tflag = true;
		tbase = addr;
		extract = 1;

		for (int i = 0; i < 5; i++) {
			if ((file.buf[i] & 0xff) != cmp[i]) {
				return null;
			}
		}

		mode = (file.buf[7] & 0xff) * 256 + (file.buf[6] & 0xff);
		if ((mode & 0x2000) != 0) {
			return null;
		} else if ((mode & 0x4000) != 0) {
			return null;
		}

		hlen = BUF + read_options(file.buf, BUF);

		file.tbase = (file.buf[9] & 0xff) * 256 + (file.buf[8] & 0xff);
		file.tlen = (file.buf[11] & 0xff) * 256 + (file.buf[10] & 0xff);
		file.tdiff = tflag ? tbase - file.tbase : 0;
		file.dbase = (file.buf[13] & 0xff) * 256 + (file.buf[12] & 0xff);
		file.dlen = (file.buf[15] & 0xff) * 256 + (file.buf[14] & 0xff);
		file.ddiff = dflag ? dbase - file.dbase : 0;
		file.bbase = (file.buf[17] & 0xff) * 256 + (file.buf[16] & 0xff);
		file.bdiff = bflag ? bbase - file.bbase : 0;
		file.zbase = (file.buf[21] & 0xff) * 256 + (file.buf[20] & 0xff);
		file.zdiff = zflag ? zbase - file.zbase : 0;

		file.segt = file.buf;
		int segtPos = hlen;
		file.segd = file.segt;
		int sedPos = segtPos + file.tlen;
		file.utab = file.segd;
		int utabPos = sedPos + file.dlen;

		file.rttab = file.utab;
		int rttabPos = utabPos + read_undef(file.utab, utabPos, file);

		file.rdtab = file.rttab;
		file.extab = file.rdtab;
		int rdtabPos = reloc_seg(file.segt, segtPos, file.tlen, file.rttab,
				rttabPos, file);
		int extabPos = reloc_seg(file.segd, sedPos, file.dlen, file.rdtab,
				rdtabPos, file);

		/*extabPos = */reloc_globals(file.extab, extabPos, file);

		if (tflag) {
			file.buf[9] = (byte) ((tbase >> 8) & 255);
			file.buf[8] = (byte) (tbase & 255);
		}
		if (dflag) {
			file.buf[13] = (byte) ((dbase >> 8) & 255);
			file.buf[12] = (byte) (dbase & 255);
		}
		if (bflag) {
			file.buf[17] = (byte) ((bbase >> 8) & 255);
			file.buf[16] = (byte) (bbase & 255);
		}
		if (zflag) {
			file.buf[21] = (byte) ((zbase >> 8) & 255);
			file.buf[20] = (byte) (zbase & 255);
		}

		file.ud = null;
		
		switch (extract) {
		case 0: /* whole file */
			return ByteBuffer.wrap(buf, 0, fsize);
		case 1: /* text segment */
			return ByteBuffer.wrap(file.segt, segtPos, file.tlen);
		case 2:
			return ByteBuffer.wrap(file.segd, sedPos, file.dlen);
		default:
			return null;
		}
	}

	private int read_options(byte[] buf, int pos) {
		int c, l = 0;

		c = buf[pos + 0] & 0xff;
		while ((c != 0)) {
			c &= 255;
			l += c;
			c = buf[pos + l] & 0xff;
		}
		return ++l;
	}

	private int read_undef(byte[] buf, int pos, File65 fp) {
		int n, l = 2;

		n = (buf[pos + 0] & 0xff) + 256 * (buf[pos + 1] & 0xff);
		fp.ud = new String[n];
		int i=0;
		while (i<n) {
			byte[] tmp = new byte[32];
			for (int j = 0; j < buf.length; j++) {
				if (buf[pos+l+j]==0) {
					fp.ud[i] = new String(tmp, 0, j);
					break;
				}
				tmp[j]=buf[pos+l+j];
			}

			while (buf[pos + (l++)] != 0) {
			}
			i++;
		}
		return l;
	}

	private int /* unsigned char */reloc_seg(byte[] buf,
			int bufPos, int len, byte[] rtab,
			int rtabPos, File65 fp) {
		int adr = -1;
		int type, seg, old, newv;
		/*
		 * printf("tdiff=%04x, ddiff=%04x, bdiff=%04x, zdiff=%04x\n", fp->tdiff,
		 * fp->ddiff, fp->bdiff, fp->zdiff);
		 */
		while (rtab[rtabPos] != 0) {
			if ((rtab[rtabPos] & 255) == 255) {
				adr += 254;
				rtabPos++;
			} else {
				adr += rtab[rtabPos] & 255;
				rtabPos++;
				type = rtab[rtabPos] & 0xe0;
				seg = rtab[rtabPos] & 0x07;
				/*
				 * printf("reloc entry @ rtab=%p (offset=%d), adr=%04x,
				 * type=%02x, seg=%d\n",rtab-1, *(rtab-1), adr, type, seg);
				 */
				rtabPos++;
				switch (type) {
				case 0x80:
					old = (buf[bufPos + adr] & 0xff) + 256 * (buf[bufPos + adr + 1] & 0xff);
					if (seg!=0) newv = old + reldiff(seg, fp);
					else newv = old + find_global(rtab, rtabPos, fp);
					buf[bufPos + adr] = (byte) (newv & 255);
					buf[bufPos + adr + 1] = (byte) ((newv >> 8) & 255);
					break;
				case 0x40:
					old = (buf[bufPos + adr] & 0xff) * 256 + (rtab[rtabPos] & 0xff);
					if (seg!=0) newv = old + reldiff(seg, fp);
					else newv = old + find_global(rtab, rtabPos, fp);
					buf[bufPos + adr] = (byte) ((newv >> 8) & 255);
					rtab[rtabPos] = (byte) (newv & 255);
					rtabPos++;
					break;
				case 0x20:
					old = buf[bufPos + adr] & 0xff;
					if (seg!=0) newv = old + reldiff(seg, fp);
					else newv = old + find_global(rtab, rtabPos, fp);
					buf[bufPos + adr] = (byte) (newv & 255);
					break;
				}
				if (seg == 0)
					rtabPos += 2;
			}
		}
		if (adr > len) {
			System.err
					.println("reloc65: Warning: relocation table entries past segment end!\n");
		}
		return ++rtabPos;
	}

	static int find_global(byte[] bp, int bpPos, File65 fp) {
		int nl = (bp[bpPos+0] & 0xff)+256*(bp[bpPos+1] & 0xff);
		String name = fp.ud[nl];		
		return fp.globals.get(name);
	}

	private int /* unsigned char */reloc_globals(
			byte[] buf, int bufPos, File65 fp) {
		int n, old, newv, seg;

		n = (buf[bufPos + 0] & 0xff) + 256 * (buf[bufPos + 1] & 0xff);
		bufPos += 2;

		while (n != 0) {
			/* printf("relocating %s, ", buf); */
			while ((buf[bufPos++]) != 0) {
			}
			seg = buf[bufPos] & 0xff;
			old = (buf[bufPos + 1] & 0xff) + 256 * (buf[bufPos + 2] & 0xff);
		    if (seg!=0) newv = old + reldiff(seg, fp);
		    else newv = old + find_global(buf, bufPos+1, fp);
			/*
			 * printf("old=%04x, seg=%d, rel=%04x, new=%04x\n", old, seg,
			 * reldiff(seg), new);
			 */
			buf[bufPos + 1] = (byte) (newv & 255);
			buf[bufPos + 2] = (byte) ((newv >> 8) & 255);
			bufPos += 3;
			n--;
		}
		return bufPos;
	}

	private int reldiff(int s, File65 fp) {
		return (((s) == 2) ? fp.tdiff : (((s) == 3) ? fp.ddiff
				: (((s) == 4) ? fp.bdiff : (((s) == 5) ? fp.zdiff : 0))));
	}
}
