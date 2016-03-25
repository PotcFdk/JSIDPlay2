/**
 *                     Sidplayer and Stereo Sidplayer format support.
 *                     ----------------------------------------------
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import libsidutils.PathUtils;
import libsidutils.Petscii;
import libsidutils.assembler.KickAssembler;

class Mus extends PSid {

	protected static String MUSDRIVER1_ASM = "/libsidplay/sidtune/musdriver1.asm";
	protected static String MUSDRIVER2_ASM = "/libsidplay/sidtune/musdriver2.asm";

	/** Known SID names. MUS loader scans for these. */
	private static final String DEFAULT_MUS_NAMES[] = new String[] { ".mus", ".str", "_a.mus", "_b.mus" };

	private static final String ERR_SIDTUNE_INVALID = "ERROR: File contains invalid data";

	private static final String SIDTUNE_2ND_INVALID = "ERROR: 2nd file contains invalid data";

	private static final int MUS_HLT_CMD = 0x14F;

	private static final int MUS_DATA_ADDR = 0x0900;

	private static final int STR_SID2_ADDR = 0xd500;

	private final KickAssembler assembler = new KickAssembler();
	/**
	 * Needed for MUS/STR player installation.
	 */
	private int musDataLen;

	@Override
	public int placeProgramInMemory(final byte[] c64buf) {
		installPlayer(c64buf);
		return super.placeProgramInMemory(c64buf);
	}

	@Override
	public void save(final String destFileName) throws IOException {
		try (FileOutputStream fMyOut = new FileOutputStream(destFileName)) {
			fMyOut.write(program);
		}
	}

	private static boolean detect(File musFile, final byte[] buffer, final int startIndex, final int[] voice3Index) {
		if (musFile != null) {
			boolean correctExtension = false;
			for (String ext : DEFAULT_MUS_NAMES) {
				if (musFile.getName().toLowerCase(Locale.ENGLISH).endsWith(ext)) {
					correctExtension = true;
					break;
				}
			}
			if (!correctExtension) {
				return false;
			}
		}
		if (buffer == null || buffer.length < startIndex + 8) {
			return false;
		}

		// Add length of voice 1 data.
		final int voice1Index = 2 + 3 * 2 + ((buffer[startIndex + 2] & 0xff) + ((buffer[startIndex + 3] & 0xff) << 8));
		// Add length of voice 2 data.
		final int voice2Index = voice1Index
				+ ((buffer[startIndex + 4] & 0xff) + ((buffer[startIndex + 5] & 0xff) << 8));
		// Add length of voice 3 data.
		voice3Index[0] = voice2Index + ((buffer[startIndex + 6] & 0xff) + ((buffer[startIndex + 7] & 0xff) << 8));

		/*
		 * validate that voice3 is still inside the buffer and that each track
		 * ends with HLT
		 */
		return voice3Index[0] - 1 < buffer.length
				&& ((buffer[voice1Index - 1] & 0xff) + ((buffer[voice1Index - 2] & 0xff) << 8)) == MUS_HLT_CMD
				&& ((buffer[voice2Index - 1] & 0xff) + ((buffer[voice2Index - 2] & 0xff) << 8)) == MUS_HLT_CMD
				&& ((buffer[voice3Index[0] - 1] & 0xff) + ((buffer[voice3Index[0] - 2] & 0xff) << 8)) == MUS_HLT_CMD;
	}

	/**
	 * Load MUS mono tune with provided meta-data contained in a PSID.
	 */
	protected static SidTune load(SidTuneInfo info, int programOffset, byte[] dataBuf) throws SidTuneError {
		Mus mus = new Mus();
		mus.info = info;
		mus.programOffset = programOffset;
		mus.loadWithProvidedMetadata(null, dataBuf);
		return mus;
	}

	/**
	 * Load MUS mono or stereo tune (find paired stereo files: MUS/STR or
	 * _A.MUS/_B.MUS)
	 */
	protected static SidTune load(final File musFile, final byte[] dataBuf) throws SidTuneError {
		final Mus mus = new Mus();
		mus.info.compatibility = Compatibility.PSIDv2;
		mus.loadWithProvidedMetadata(musFile, dataBuf);
		return mus;
	}

	private void loadWithProvidedMetadata(final File musFile, final byte[] musBuf) throws SidTuneError {
		final int[] voice3Index = new int[1];
		if (!detect(musFile, musBuf, programOffset, voice3Index)) {
			throw new SidTuneError(ERR_SIDTUNE_INVALID);
		}

		info.songs = info.startSong = 1;

		musDataLen = musBuf.length;
		info.loadAddr = MUS_DATA_ADDR;

		int infoStringLocation = voice3Index[0];

		/*
		 * FIXME: dealing with credits is problematic. The data is unstructured,
		 * and longer than fits into DB index. We could assume that first line
		 * is title, second is author, third is release, but that's actually not
		 * very likely.
		 */
		{
			final String credit = Petscii
					.petsciiToIso88591(Arrays.copyOfRange(musBuf, infoStringLocation, musBuf.length));
			infoStringLocation += credit.length() + 1;
			info.commentString.add(credit);
		}

		byte[] strBuf = null;
		File stereoFile = null;
		if (musFile != null) {
			stereoFile = getStereoTune(musFile);
			if (stereoFile != null) {
				try {
					strBuf = getFileContents(stereoFile);
				} catch (IOException e) {
					// ignore missing stereo tune
				}
			}
		}

		// If we appear to have additional data at the end, check is it's
		// another mus file (but only if a second file isn't supplied)
		if (strBuf == null && infoStringLocation < musBuf.length) {
			strBuf = Arrays.copyOfRange(musBuf, infoStringLocation, musBuf.length);
		}

		if (strBuf != null) {
			if (!detect(stereoFile, strBuf, 0, voice3Index)) {
				throw new SidTuneError(SIDTUNE_2ND_INVALID);
			}

			infoStringLocation = voice3Index[0];

			{
				final String credit = Petscii
						.petsciiToIso88591(Arrays.copyOfRange(strBuf, infoStringLocation, strBuf.length));
				infoStringLocation += credit.length() + 1;
				info.commentString.add(credit);
			}

			info.sidChipBase2 = STR_SID2_ADDR;
			info.initAddr = 0xfc90;
			info.playAddr = 0xfc96;
		} else {
			info.initAddr = 0xec60;
			info.playAddr = 0xec80;
		}

		info.infoString.add(PathUtils.getFilenameWithoutSuffix(musFile.getName()));
		info.infoString.add("<?>");
		info.infoString.add("<?>");

		program = new byte[musBuf.length + (strBuf != null ? strBuf.length : 0)];
		info.c64dataLen = program.length - programOffset;
		System.arraycopy(musBuf, 0, program, 0, musBuf.length);
		if (strBuf != null) {
			System.arraycopy(strBuf, 0, program, musBuf.length, strBuf.length);
		}

		findPlaceForDriver();
	}

	/**
	 * Get stereo music file by naming convention. Couples are *.mus/*.str or
	 * *_a.mus/*_b.mus .
	 * 
	 * @param file
	 *            file to get the stereo tune for.
	 * @return stereo file
	 */
	private static File getStereoTune(final File file) {
		final String fileName = file.getName();
		final File[] siblings = file.getParentFile().listFiles((dir, name) -> {
			for (String ext : DEFAULT_MUS_NAMES) {
				if (name.toLowerCase(Locale.ENGLISH).endsWith(ext)) {
					return true;
				}
			}
			return false;
		});
		for (String extension : DEFAULT_MUS_NAMES) {
			String test = fileName.replaceFirst("(_[aA]|_[bB])?\\.\\w+$", extension);
			if (!fileName.equalsIgnoreCase(test)) {
				for (File sibling : siblings) {
					if (sibling.getName().equalsIgnoreCase(test)) {
						return sibling;
					}
				}
			}
		}
		return null;
	}

	private void installPlayer(final byte[] c64buf) {
		{
			HashMap<String, String> globals = new HashMap<String, String>();
			InputStream asm = Mus.class.getResourceAsStream(MUSDRIVER1_ASM);
			byte[] driver = assembler.assemble(MUSDRIVER1_ASM, asm, globals);
			Integer data_low = assembler.getLabels().get("data_low");
			Integer data_high = assembler.getLabels().get("data_high");
			if (data_low == null) {
				throw new RuntimeException("Label data_low not found in " + MUSDRIVER1_ASM);
			}
			if (data_high == null) {
				throw new RuntimeException("Label data_high not found in " + MUSDRIVER1_ASM);
			}

			// Install MUS player #1.
			int dest = (driver[0] & 0xff) + ((driver[1] & 0xff) << 8);
			System.arraycopy(driver, 2, c64buf, dest, driver.length - 2);
			// Point player #1 to data #1.
			c64buf[data_low+1] = MUS_DATA_ADDR + 2 & 0xFF;
			c64buf[data_high+1] = MUS_DATA_ADDR + 2 >> 8;
		}
		if (info.sidChipBase2 != 0) {
			HashMap<String, String> globals = new HashMap<String, String>();
			InputStream asm = Mus.class.getResourceAsStream(MUSDRIVER2_ASM);
			byte[] driver = assembler.assemble(MUSDRIVER2_ASM, asm, globals);

			// Install MUS player #2.
			int dest = (driver[0] & 0xff) + ((driver[1] & 0xff) << 8);
			System.arraycopy(driver, 2, c64buf, dest, driver.length - 2);
			// Point player #2 to data #2.
			Integer data_low = assembler.getLabels().get("data_low");
			Integer data_high = assembler.getLabels().get("data_high");
			if (data_low == null) {
				throw new RuntimeException("Label data_low not found in " + MUSDRIVER2_ASM);
			}
			if (data_high == null) {
				throw new RuntimeException("Label data_high not found in " + MUSDRIVER2_ASM);
			}
			c64buf[data_low+1] = (byte) (MUS_DATA_ADDR + musDataLen + 2 & 0xFF);
			c64buf[data_high+1] = (byte) (MUS_DATA_ADDR + musDataLen + 2 >> 8);
		}
	}

	@Override
	public String getMD5Digest() {
		return null;
	}

}
