/**
 *                         C64 PRG file format support.
 *                         ----------------------------
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import libsidutils.PathUtils;
import libsidutils.SidIdV2;

class Prg extends SidTune {

	private static final SidIdV2 sidId = new SidIdV2();

	static {
		sidId.readconfig();
		sidId.setMultiScan(true);
	}
	protected int fileOffset;

	private static final String _sidtune_truncated = "ERROR: File is most likely truncated";

	protected byte[] program;

	protected static SidTune load(final String path, final byte[] dataBuf) throws SidTuneError {
		if (!PathUtils.getExtension(path).equalsIgnoreCase(".prg")) {
			return null;
		}
		final Prg sidtune = new Prg();
		if (dataBuf.length < 2) {
			throw new SidTuneError(_sidtune_truncated);
		}

		// Automatic settings
		sidtune.fileOffset = 2;
		sidtune.info.loadAddr = (dataBuf[0] & 0xff) + ((dataBuf[1] & 0xff) << 8);
		sidtune.info.c64dataLen = dataBuf.length - 2;
		sidtune.program = dataBuf;

		// Create the speed/clock setting table.
		sidtune.convertOldStyleSpeedToTables(~0);
		return sidtune;
	}

	@Override
	public void save(final String destFileName, final boolean overWriteFlag) throws IOException {
		try (FileOutputStream fMyOut = new FileOutputStream(destFileName,
				!overWriteFlag)) {
			fMyOut.write(program);
		}
	}

	@Override
	public int placeProgramInMemory(final byte[] c64buf) {
		final int start = info.loadAddr;
		final int end = start + info.c64dataLen;
		c64buf[0x2d] = (byte) (end & 0xff);
		c64buf[0x2e] = (byte) (end >> 8); // Variables start
		c64buf[0x2f] = (byte) (end & 0xff);
		c64buf[0x30] = (byte) (end >> 8); // Arrays start
		c64buf[0x31] = (byte) (end & 0xff);
		c64buf[0x32] = (byte) (end >> 8); // Strings start
		c64buf[0xac] = (byte) (start & 0xff);
		c64buf[0xad] = (byte) (start >> 8);
		c64buf[0xae] = (byte) (end & 0xff);
		c64buf[0xaf] = (byte) (end >> 8);

		// Copy data from cache to the correct destination.
		System.arraycopy(program, fileOffset, c64buf, info.loadAddr, info.c64dataLen);
		return -1;
	}

	/**
	 * Identify the player IDs of a program in memory.
	 * 
	 * @return the player IDs as a list
	 */
	@Override
	public Collection<String> identify() {
		return sidId.identify(program);
	}
	
	@Override
	public long getInitDelay() {
		/* Wait 2.5 seconds before initializing PRG/P00. */
		return 2500000;
	}

}