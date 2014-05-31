/**
 *                          C64 P00 file format support.
 *                          ----------------------------
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package libsidplay.sidtune;

import java.nio.ByteBuffer;
import java.util.Locale;

import libsidutils.PathUtils;

/**
 * File format from PC64. PC64 automatically generates the filename from the cbm
 * name (16 to 8 conversion) but we only need to worry about that when writing
 * files should we want pc64 compatibility. The extension numbers are just an
 * index to try to avoid repeats. Name conversion works by creating an initial
 * filename from alphanumeric and ' ', '-' characters only with the later two
 * being converted to '_'. Then it parses the filename from end to start
 * removing characters stopping as soon as the filename becomes <= 8. The
 * removal of characters occurs in three passes, the first removes all '_', then
 * vowels and finally numerics. If the filename is still greater than 8 it is
 * truncated. struct X00Header
 * 
 * @author Ken Händel
 * 
 */
class P00 extends Prg {

	private static final String SIDTUNE_ID = "C64File";

	private enum X00Format {
		/** DEL */
		D,
		/** SEQ */
		S,
		/** PRG */
		P,
		/** USR */
		U,
		/** REL */
		R
	}

	private static class X00Header {
		private static final int ID_LEN = 8;
		private static final int NAME_LEN = 17;
		private static final int SIZE = 26;

		public X00Header(final byte[] s) {
			final ByteBuffer b = ByteBuffer.wrap(s);
			b.get(id);
			b.get(name);
		}

		private byte id[] = new byte[ID_LEN];

		private byte name[] = new byte[NAME_LEN];

		public Object getId() {
			return new String(id, 0, 7);
		}
	}

	protected static SidTune load(final String name, final byte[] dataBuf)
			throws SidTuneError {
		String ext = PathUtils.getExtension(name).toUpperCase(Locale.ENGLISH);
		if (dataBuf.length < X00Header.SIZE + 2 || ext.length() != 4
				|| '0' != ext.charAt(2) || '0' != ext.charAt(3)) {
			throw new SidTuneError("Bad file extension expected: .p00");
		}
		final X00Header header = new X00Header(dataBuf);
		X00Format type = X00Format.valueOf(String.valueOf(ext.charAt(1)));
		if (type != X00Format.P || !header.getId().equals(SIDTUNE_ID)) {
			throw new SidTuneError("Bad program type, expected: C64File, PRG");
		}
		final P00 p00 = new P00();
		p00.program = dataBuf;
		p00.programOffset = X00Header.SIZE + 2;
		p00.info.c64dataLen = dataBuf.length - p00.programOffset;
		p00.info.loadAddr = (dataBuf[X00Header.SIZE] & 0xff)
				| ((dataBuf[X00Header.SIZE + 1] & 0xff) << 8);

		p00.info.infoString.add(convertPetsciiToAscii(header.name, 0));

		p00.convertOldStyleSpeedToTables(~0);
		return p00;
	}
}