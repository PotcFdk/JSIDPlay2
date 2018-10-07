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

import static libsidplay.sidtune.SidTune.Compatibility.PSIDv2;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import libsidutils.PathUtils;
import libsidutils.Petscii;
import libsidutils.assembler.KickAssembler;
import libsidutils.assembler.KickAssemblerResult;

class Mus extends PSid {

	private static final String MUSDRIVER1_ASM = "/libsidplay/sidtune/musdriver1.asm";
	private static final String MUS_DRIVER1_BIN = "/libsidplay/sidtune/musdriver1.bin";
	private static final String MUSDRIVER2_ASM = "/libsidplay/sidtune/musdriver2.asm";
	private static final String MUS_DRIVER2_BIN = "/libsidplay/sidtune/musdriver2.bin";

	/** Known SID names. MUS loader scans for these. */
	private static final List<String> DEFAULT_MUS_NAMES = Arrays.asList(".mus", ".str", "_a.mus", "_b.mus");

	private static final String ERR_SIDTUNE_INVALID = "MUS: file contains invalid data";

	private static final String ERR_SIDTUNE_2ND_INVALID = "MUS: 2nd file contains invalid data";

	private static final int MUS_HLT_CMD = 0x14F;

	private static final int MUS_DATA_ADDR = 0x0900;

	private static final int DUAL_SID_BASE = 0xd500;

	/**
	 * Needed for MUS/STR player installation.
	 */
	private int musDataLen;

	private KickAssemblerResult preparedDriver;
	private KickAssemblerResult preparedStereoDriver;

	@Override
	public Integer placeProgramInMemory(final byte[] c64buf) {
		if (USE_KICKASSEMBLER) {
			assembleAndinstallMusPlayers(c64buf);
		} else {
			relocateAndInstallMusPlayers(c64buf);
		}
		return super.placeProgramInMemory(c64buf);
	}

	private int detect(File musFile, final byte[] buffer, final String errorMessage) throws SidTuneError {
		String suffix = PathUtils.getFilenameSuffix(musFile.getName().toLowerCase(Locale.ENGLISH));
		if (!DEFAULT_MUS_NAMES.stream().anyMatch(ext -> suffix.endsWith(ext)))
			throw new SidTuneError(errorMessage);
		if (buffer == null || buffer.length < 8) {
			throw new SidTuneError(errorMessage);
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
			throw new SidTuneError(errorMessage);
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
		int voice3DataEnd = detect(musFile, musBuf, ERR_SIDTUNE_INVALID);

		musDataLen = musBuf.length;

		/*
		 * FIXME: dealing with credits is problematic. The data is unstructured, and
		 * longer than fits into DB index. We could assume that first line is title,
		 * second is author, third is release, but that's actually not very likely.
		 */
		if (voice3DataEnd < musBuf.length) {
			String credits = getCredits(musBuf, voice3DataEnd);
			info.commentString.add(credits);
			voice3DataEnd += credits.length() + 1;
		}

		// load stereo tune as well, if available
		byte[] strBuf = null;
		File stereoFile = getStereoTune(musFile);
		if (stereoFile != null) {
			try {
				strBuf = getContents(stereoFile);
			} catch (IOException e) {
				// ignore missing stereo tune
			}
		}

		if (strBuf != null) {
			voice3DataEnd = detect(stereoFile, strBuf, ERR_SIDTUNE_2ND_INVALID);

			if (voice3DataEnd < strBuf.length) {
				info.commentString.add(getCredits(strBuf, voice3DataEnd));
			}

			info.sidChipBase[1] = DUAL_SID_BASE;
		}

		info.loadAddr = MUS_DATA_ADDR;
		info.compatibility = PSIDv2;
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
	 * @param file file to get the stereo tune for (e.g. name.mus).
	 * @return stereo file (e.g. name.str)
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
			// ... a corresponding stereo file with a different extension ...
			if (!file.getName().equalsIgnoreCase(test)) {
				for (File sibling : siblings) {
					// ... which matches a siblings name
					// (e.g. MUS/STR, _A.MUS, _B.MUS)
					if (sibling.getName().equalsIgnoreCase(test)) {
						return sibling;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Linux ALSA is very sensible for timing: therefore we assemble before we open
	 * AudioLine
	 */
	public void prepare() {
		if (USE_KICKASSEMBLER) {
			KickAssembler assembler = new KickAssembler();

			// Assemble MUS player 1
			InputStream asm = Mus.class.getResourceAsStream(MUSDRIVER1_ASM);
			preparedDriver = assembler.assemble(MUSDRIVER1_ASM, asm, new HashMap<String, String>());
			// Install MUS player 1
			checkLabels(MUSDRIVER1_ASM, preparedDriver.getResolvedSymbols(), "data_low", "data_high", "init", "start");
			info.initAddr = preparedDriver.getResolvedSymbols().get("init");
			info.playAddr = preparedDriver.getResolvedSymbols().get("start");

			if (info.getSIDChipBase(1) != 0) {
				// Assemble MUS player 2
				InputStream stereoAsm = Mus.class.getResourceAsStream(MUSDRIVER2_ASM);
				preparedStereoDriver = assembler.assemble(MUSDRIVER2_ASM, stereoAsm, new HashMap<String, String>());
				// Install MUS player 2
				checkLabels(MUSDRIVER2_ASM, preparedStereoDriver.getResolvedSymbols(), "data_low", "data_high", "init",
						"start");
				info.initAddr = preparedStereoDriver.getResolvedSymbols().get("init");
				info.playAddr = preparedStereoDriver.getResolvedSymbols().get("start");
			}
			super.prepare();
		}
	}

	private void assembleAndinstallMusPlayers(final byte[] c64buf) {
		if (preparedDriver == null) {
			prepare();
		}
		installMusPlayer(c64buf, MUS_DATA_ADDR, preparedDriver.getData(),
				preparedDriver.getResolvedSymbols().get("data_low"),
				preparedDriver.getResolvedSymbols().get("data_high"));

		if (info.getSIDChipBase(1) != 0) {
			installMusPlayer(c64buf, MUS_DATA_ADDR + musDataLen, preparedStereoDriver.getData(),
					preparedStereoDriver.getResolvedSymbols().get("data_low"),
					preparedStereoDriver.getResolvedSymbols().get("data_high"));
		}
	}

	private void checkLabels(String asmSource, Map<String, Integer> resolvedSymbols, String data_low, String data_high,
			String init, String start) {
		if (resolvedSymbols.get(data_low) == null) {
			throw new RuntimeException("Symbol data_low not found in " + asmSource);
		}
		if (resolvedSymbols.get(data_high) == null) {
			throw new RuntimeException("Symbol data_high not found in " + asmSource);
		}
		if (resolvedSymbols.get(init) == null) {
			throw new RuntimeException("Symbol init not found in " + asmSource);
		}
		if (resolvedSymbols.get(start) == null) {
			throw new RuntimeException("Symbol start not found in " + asmSource);
		}
	}

	private void relocateAndInstallMusPlayers(byte[] c64buf) {
		// Install MUS player #1
		byte[] MUS_DRIVER1;
		try (DataInputStream is = new DataInputStream(Mus.class.getResourceAsStream(MUS_DRIVER1_BIN))) {
			URL url = Mus.class.getResource(MUS_DRIVER1_BIN);
			MUS_DRIVER1 = new byte[url.openConnection().getContentLength()];
			is.readFully(MUS_DRIVER1);
		} catch (IOException e) {
			throw new RuntimeException("Load failed for resource: " + MUS_DRIVER1_BIN);
		}
		int dest = (MUS_DRIVER1[0] & 0xff) + ((MUS_DRIVER1[1] & 0xff) << 8);
		installMusPlayer(c64buf, MUS_DATA_ADDR, MUS_DRIVER1, dest + 0xc6e - 1, dest + 0xc70 - 1);

		info.initAddr = 0xec60;
		info.playAddr = 0xec80;

		if (info.getSIDChipBase(1) != 0) {
			// Install MUS player #2
			byte[] MUS_DRIVER2;
			try (DataInputStream is = new DataInputStream(Mus.class.getResourceAsStream(MUS_DRIVER2_BIN))) {
				URL url = Mus.class.getResource(MUS_DRIVER2_BIN);
				MUS_DRIVER2 = new byte[url.openConnection().getContentLength()];
				is.readFully(MUS_DRIVER2);
			} catch (IOException e) {
				throw new RuntimeException("Load failed for resource: " + MUS_DRIVER2_BIN);
			}
			dest = (MUS_DRIVER2[0] & 0xff) + ((MUS_DRIVER2[1] & 0xff) << 8);
			installMusPlayer(c64buf, MUS_DATA_ADDR + musDataLen, MUS_DRIVER2, dest + 0xc6e - 1, dest + 0xc70 - 1);

			info.initAddr = 0xfc90;
			info.playAddr = 0xfc96;
		}
	}

	private void installMusPlayer(final byte[] c64buf, final int musDataAddr, final byte[] driver,
			final Integer data_low, final Integer data_high) {
		// Install MUS player
		int driverAddr = (driver[0] & 0xff) + ((driver[1] & 0xff) << 8);
		System.arraycopy(driver, 2, c64buf, driverAddr, driver.length - 2);

		// Point MUS player to data.
		c64buf[data_low + 1] = (byte) (2 + musDataAddr & 0xFF);
		c64buf[data_high + 1] = (byte) (2 + musDataAddr >> 8);

	}

	@Override
	public void save(final String destFileName) throws IOException {
		try (FileOutputStream fMyOut = new FileOutputStream(destFileName)) {
			fMyOut.write(program);
		}
	}

}
