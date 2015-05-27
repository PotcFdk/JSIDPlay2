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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import javafx.scene.image.Image;
import libsidutils.PathUtils;
import libsidutils.SidIdInfo;
import libsidutils.SidIdInfo.PlayerInfoSection;
import libsidutils.SidIdV2;

class Prg extends SidTune {

	protected static final MessageDigest MD5_DIGEST;
	static {
		try {
			MD5_DIGEST = MessageDigest.getInstance("MD5");
		} catch (final NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final SidIdV2 SID_ID = new SidIdV2();
	static {
		try {
			SID_ID.readconfig();
			SID_ID.setMultiScan(true);
		} catch (NumberFormatException | IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final SidIdInfo SID_ID_INFO = new SidIdInfo();
	static {
		try {
			SID_ID_INFO.readconfig();
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	protected int programOffset;

	protected byte[] program;

	protected static SidTune load(final String name, final byte[] dataBuf)
			throws SidTuneError {
		if (!PathUtils.getExtension(name).equalsIgnoreCase(".prg")
				|| dataBuf.length < 2) {
			throw new SidTuneError("Bad file extension expected: .prg and length > 2");
		}
		final Prg prg = new Prg();
		
		prg.program = dataBuf;
		prg.programOffset = 2;
		prg.info.c64dataLen = dataBuf.length - prg.programOffset;
		prg.info.loadAddr = (dataBuf[0] & 0xff) + ((dataBuf[1] & 0xff) << 8);

		prg.info.infoString.add(PathUtils.getBaseNameNoExt(name));
		
		prg.convertOldStyleSpeedToTables(~0);
		return prg;
	}

	@Override
	public void save(final String filename, final boolean overwrite)
			throws IOException {
		try (FileOutputStream out = new FileOutputStream(filename, !overwrite)) {
			out.write(program);
		}
	}

	@Override
	public int placeProgramInMemory(final byte[] mem) {
		final int start = info.loadAddr;
		final int end = start + info.c64dataLen;
		mem[0x2d] = (byte) (end & 0xff);
		mem[0x2e] = (byte) (end >> 8); // Variables start
		mem[0x2f] = (byte) (end & 0xff);
		mem[0x30] = (byte) (end >> 8); // Arrays start
		mem[0x31] = (byte) (end & 0xff);
		mem[0x32] = (byte) (end >> 8); // Strings start
		mem[0xac] = (byte) (start & 0xff);
		mem[0xad] = (byte) (start >> 8);
		mem[0xae] = (byte) (end & 0xff);
		mem[0xaf] = (byte) (end >> 8);

		// Copy data from cache to the correct destination.
		System.arraycopy(program, programOffset, mem, info.loadAddr,
				info.c64dataLen);
		return -1;
	}

	/**
	 * Identify the player IDs of a program in memory.
	 * 
	 * @return the player IDs as a list
	 */
	@Override
	public Collection<String> identify() {
		return SID_ID.identify(program);
	}

	/**
	 * Search player ID Info.
	 * 
	 * @param playerName
	 *            player to get infos for
	 * @return player infos (or null, if not found)
	 */
	@Override
	public PlayerInfoSection getPlayerInfo(String playerName) {
		return SID_ID_INFO.getPlayerInfo(playerName);
	}

	@Override
	public String getMD5Digest() {
		byte[] myMD5 = new byte[info.c64dataLen];
		System.arraycopy(program, programOffset, myMD5, 0, info.c64dataLen);
		StringBuilder md5 = new StringBuilder();
		for (final byte encryptMsg : MD5_DIGEST.digest(myMD5)) {
			md5.append(String.format("%02x", encryptMsg & 0xff));
		}
		return md5.toString();
	}

	@Override
	public long getInitDelay() {
		/* Wait 2.5 seconds before initializing PRG/P00. */
		return 2500000;
	}

	@Override
	public Image getImage() {
		return null;
	}
}