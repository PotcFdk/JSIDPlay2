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
import java.util.List;
import java.util.Locale;

import libsidutils.PathUtils;
import libsidutils.Petscii;
import libsidutils.assembler.KickAssembler;

class Mus extends PSid {

	protected static String MUSDRIVER1_ASM = "/libsidplay/sidtune/musdriver1.asm";
	protected static String MUSDRIVER2_ASM = "/libsidplay/sidtune/musdriver2.asm";

	/** Known SID names. MUS loader scans for these. */
	private static final List<String> DEFAULT_MUS_NAMES = Arrays.asList(".mus", ".str", "_a.mus", "_b.mus");

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
		installMusPlayers(c64buf);
		return super.placeProgramInMemory(c64buf);
	}

	private int detect(File musFile, final byte[] buffer, final boolean isStereoTune) throws SidTuneError {
		String suffix = PathUtils.getFilenameSuffix(musFile.getName().toLowerCase(Locale.ENGLISH));
		if (!DEFAULT_MUS_NAMES.stream().anyMatch(ext -> suffix.endsWith(ext)))
			throw new SidTuneError(isStereoTune ? SIDTUNE_2ND_INVALID : ERR_SIDTUNE_INVALID);
		if (buffer == null || buffer.length < 8) {
			throw new SidTuneError(isStereoTune ? SIDTUNE_2ND_INVALID : ERR_SIDTUNE_INVALID);
		}
		// Add length of voice 1 data.
		final int voice1DataEnd = 2 + 3 * 2 + ((buffer[2] & 0xff) + ((buffer[3] & 0xff) << 8));
		// Add length of voice 2 data.
		final int voice2DataEnd = voice1DataEnd + ((buffer[4] & 0xff) + ((buffer[5] & 0xff) << 8));
		// Add length of voice 3 data.
		final int voice3DataEnd = voice2DataEnd + ((buffer[6] & 0xff) + ((buffer[7] & 0xff) << 8));

		// validate that voice3 is still inside the buffer and that each track
		// ends with HLT
		if (!(voice3DataEnd - 1 < buffer.length
				&& ((buffer[voice1DataEnd - 1] & 0xff) + ((buffer[voice1DataEnd - 2] & 0xff) << 8)) == MUS_HLT_CMD
				&& ((buffer[voice2DataEnd - 1] & 0xff) + ((buffer[voice2DataEnd - 2] & 0xff) << 8)) == MUS_HLT_CMD
				&& ((buffer[voice3DataEnd - 1] & 0xff) + ((buffer[voice3DataEnd - 2] & 0xff) << 8)) == MUS_HLT_CMD)) {
			throw new SidTuneError(isStereoTune ? SIDTUNE_2ND_INVALID : ERR_SIDTUNE_INVALID);
		}
		return voice3DataEnd;
	}

	/**
	 * Load MUS mono or stereo tune (find paired stereo files: MUS/STR or
	 * _A.MUS/_B.MUS)
	 */
	protected static SidTune load(final File musFile, final byte[] dataBuf) throws SidTuneError {
		final Mus mus = new Mus();
		mus.loadWithProvidedMetadata(musFile, dataBuf);
		return mus;
	}

	private void loadWithProvidedMetadata(final File musFile, final byte[] musBuf) throws SidTuneError {
		int voice3DataEnd = detect(musFile, musBuf, false);

		musDataLen = musBuf.length;

		/*
		 * FIXME: dealing with credits is problematic. The data is unstructured,
		 * and longer than fits into DB index. We could assume that first line
		 * is title, second is author, third is release, but that's actually not
		 * very likely.
		 */
		if (voice3DataEnd < musBuf.length - 1) {
			String credits = getCredits(musBuf, voice3DataEnd);
			info.commentString.add(credits);
			voice3DataEnd += credits.length() + 1;
		}

		// load stereo tune as well, if available
		byte[] strBuf = null;
		File stereoFile = getStereoTune(musFile);
		if (stereoFile != null) {
			try {
				strBuf = getFileContents(stereoFile);
			} catch (IOException e) {
				// ignore missing stereo tune
			}
		}

		if (strBuf != null) {
			voice3DataEnd = detect(musFile, musBuf, true);

			if (voice3DataEnd < strBuf.length - 1) {
				info.commentString.add(getCredits(strBuf, voice3DataEnd));
			}

			info.sidChipBase2 = STR_SID2_ADDR;
			info.initAddr = 0xfc90;
			info.playAddr = 0xfc96;
		} else {
			info.initAddr = 0xec60;
			info.playAddr = 0xec80;
		}

		info.loadAddr = MUS_DATA_ADDR;
		info.songs = info.startSong = 1;
		info.compatibility = Compatibility.PSIDv2;
		info.infoString.add(PathUtils.getFilenameWithoutSuffix(musFile.getName()));
		info.infoString.add("<?>");
		info.infoString.add("<?>");

		program = new byte[musBuf.length + (strBuf != null ? strBuf.length : 0)];
		info.c64dataLen = program.length;
		System.arraycopy(musBuf, 0, program, 0, musBuf.length);
		if (strBuf != null) {
			System.arraycopy(strBuf, 0, program, musBuf.length, strBuf.length);
		}
		findPlaceForDriver();
	}

	private String getCredits(byte[] musBuf, int voice3DataEnd) {
		byte[] creditsBytes = Arrays.copyOfRange(musBuf, voice3DataEnd, musBuf.length - 1);
		return Petscii.petsciiToIso88591(creditsBytes);
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
		// Get all sibling files
		final File[] siblings = file.getParentFile().listFiles((dir, name) -> {
			String suffix = PathUtils.getFilenameSuffix(name.toLowerCase(Locale.ENGLISH));
			return DEFAULT_MUS_NAMES.stream().filter(ext -> suffix.endsWith(ext)).findFirst().isPresent();
		});
		// For each possible MUS/STR extension, check if there is ...
		for (String extension : DEFAULT_MUS_NAMES) {
			String test = file.getName().replaceFirst("(_[aA]|_[bB])?\\.\\w+$", extension);
			// ... a corresponding stereo file with a different extension
			// (e.g. MUS/STR, _A.MUS, _B.MUS)
			if (!file.getName().equalsIgnoreCase(test)) {
				for (File sibling : siblings) {
					if (sibling.getName().equalsIgnoreCase(test)) {
						return sibling;
					}
				}
			}
		}
		return null;
	}

	private void installMusPlayers(final byte[] c64buf) {
		{
			// Assemble MUS player 1
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
			int driverAddr = (driver[0] & 0xff) + ((driver[1] & 0xff) << 8);
			System.arraycopy(driver, 2, c64buf, driverAddr, driver.length - 2);

			// Point player #1 to data #1.
			c64buf[data_low + 1] = MUS_DATA_ADDR + 2 & 0xFF;
			c64buf[data_high + 1] = MUS_DATA_ADDR + 2 >> 8;
		}
		if (info.sidChipBase2 != 0) {
			// Assemble MUS player 2
			HashMap<String, String> globals = new HashMap<String, String>();
			InputStream asm = Mus.class.getResourceAsStream(MUSDRIVER2_ASM);
			byte[] driver = assembler.assemble(MUSDRIVER2_ASM, asm, globals);

			// Install MUS player #2.
			int driverAddr = (driver[0] & 0xff) + ((driver[1] & 0xff) << 8);
			System.arraycopy(driver, 2, c64buf, driverAddr, driver.length - 2);

			// Point player #2 to data #2.
			Integer data_low = assembler.getLabels().get("data_low");
			Integer data_high = assembler.getLabels().get("data_high");
			if (data_low == null) {
				throw new RuntimeException("Label data_low not found in " + MUSDRIVER2_ASM);
			}
			if (data_high == null) {
				throw new RuntimeException("Label data_high not found in " + MUSDRIVER2_ASM);
			}
			c64buf[data_low + 1] = (byte) (MUS_DATA_ADDR + musDataLen + 2 & 0xFF);
			c64buf[data_high + 1] = (byte) (MUS_DATA_ADDR + musDataLen + 2 >> 8);
		}
	}

	@Override
	public String getMD5Digest() {
		return null;
	}

	@Override
	public void save(final String destFileName) throws IOException {
		try (FileOutputStream fMyOut = new FileOutputStream(destFileName)) {
			fMyOut.write(program);
		}
	}

}
