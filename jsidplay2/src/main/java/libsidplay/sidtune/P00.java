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

import libsidutils.PathUtils;

class P00 extends Prg {

	private static final int X00_ID_LEN = 8;

	private static final int X00_NAME_LEN = 17;

	/**
	 * File format from PC64. PC64 automatically generates the filename from the
	 * cbm name (16 to 8 conversion) but we only need to worry about that when
	 * writing files should we want pc64 compatibility. The extension numbers
	 * are just an index to try to avoid repeats. Name conversion works by
	 * creating an initial filename from alphanumeric and ' ', '-' characters
	 * only with the later two being converted to '_'. Then it parses the
	 * filename from end to start removing characters stopping as soon as the
	 * filename becomes <= 8. The removal of characters occurs in three passes,
	 * the first removes all '_', then vowels and finally numerics. If the
	 * filename is still greater than 8 it is truncated. struct X00Header
	 * 
	 * @author Ken Händel
	 * 
	 */
	private static class X00Header {
		public static final int SIZE = 26;

		public X00Header(final byte[] s) {
			final ByteBuffer b = ByteBuffer.wrap(s);
			b.get(id);
			b.get(name);
		}

		/**
		 * C64File
		 */
		public byte id[] = new byte[X00_ID_LEN];

		/**
		 * C64 name
		 */
		public byte name[] = new byte[X00_NAME_LEN];
	}

	private enum X00Format {
		X00_UNKNOWN, X00_DEL, X00_SEQ, X00_PRG, X00_USR, X00_REL
	}

	private static final String _sidtune_id = "C64File";

	private static final String _sidtune_truncated = "ERROR: File is most likely truncated";

	protected static SidTune load(final String path, final byte[] dataBuf) throws SidTuneError {
		String ext = PathUtils.getExtension(path);
		if (dataBuf.length < X00Header.SIZE) {
			return null;
		}
		final X00Header pHeader = new X00Header(dataBuf);
		final int bufLen = dataBuf.length;

		// Combined extension & magic field identification
		if (ext.length() != 4) {
			return null;
		}
		if (!Character.isDigit(ext.charAt(2))
				|| !Character.isDigit(ext.charAt(3))) {
			return null;
		}

		X00Format type = X00Format.X00_UNKNOWN;
		switch (Character.toUpperCase(ext.charAt(1))) {
		case 'D':
			type = X00Format.X00_DEL;
			break;
		case 'S':
			type = X00Format.X00_SEQ;
			break;
		case 'P':
			type = X00Format.X00_PRG;
			break;
		case 'U':
			type = X00Format.X00_USR;
			break;
		case 'R':
			type = X00Format.X00_REL;
			break;
		}

		if (type == X00Format.X00_UNKNOWN) {
			return null;
		}

		// Verify the file is what we think it is
		if (bufLen < X00_ID_LEN) {
			return null;
		} else if (!new String(pHeader.id, 0, 7).equals(_sidtune_id)) {
			return null;
		}

		final P00 sidtune = new P00();

		// File types current supported
		if (type != X00Format.X00_PRG) {
			throw new SidTuneError("Not a PRG inside X00");
		}

		if (bufLen < X00Header.SIZE + 2) {
			throw new SidTuneError(_sidtune_truncated);
		}

		sidtune.info.infoString.add(convertPetsciiToAscii(pHeader.name, 0));

		// Automatic settings
		sidtune.fileOffset = X00Header.SIZE + 2;
		sidtune.info.loadAddr = (dataBuf[X00Header.SIZE] & 0xff)
				| ((dataBuf[X00Header.SIZE + 1] & 0xff) << 8);
		sidtune.info.c64dataLen = dataBuf.length - 2 - X00Header.SIZE;
		sidtune.info.songs = 1;
		sidtune.info.startSong = 1;
		sidtune.program = dataBuf;

		// Create the speed/clock setting table.
		sidtune.convertOldStyleSpeedToTables(~0);

		return sidtune;
	}
}