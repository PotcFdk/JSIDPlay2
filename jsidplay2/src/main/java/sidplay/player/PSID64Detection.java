package sidplay.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidutils.Petscii;

public final class PSID64Detection {

	public static PSid64DetectedTuneInfo detectPSid64TuneInfo(byte[] ram, int videoScreenAddress) {
		boolean detected = false;
		CPUClock cpuClock = null;
		List<ChipModel> chipModels = new ArrayList<>();
		int stereoAddress = 0;
		int row = 2;
		int col = 6;
		// Search for PSID64 on video screen
		if (checkScreenMessage(ram, videoScreenAddress, "PSID64", row, col)) {
			detected = true;
			// start searching at line 12, column=10
			row = 12;
			col = 10;
			// e.g. "PAL, 8580, 8580 at $D500"
			// or "NTSC, MOS8580, MOS8580 at $D500"
			// search for PAL/NTSC
			if (checkScreenMessage(ram, videoScreenAddress, "NTSC", row, col)) {
				cpuClock = CPUClock.NTSC;
				// NTSC one char longer than PAL
				col++;
			} else {
				cpuClock = CPUClock.PAL;
			}
			// Search for MOS6581 or MOS8580 for mono SID chip model
			String chipModel = detectChipModel6581or8580(ram, videoScreenAddress, row, col + 5);
			if (chipModel != null) {
				chipModels.add(toChipModel(chipModel));
			}
			// Search for MOS6581 or MOS8580 for stereo SID chip model
			if (chipModel != null && chipModel.length() == "MOSXXXX".length()) {
				// MOS8580 three chars longer than 8580
				col += 3;
			}
			chipModel = detectChipModel6581or8580(ram, videoScreenAddress, row, col + 11);
			if (chipModel != null) {
				chipModels.add(toChipModel(chipModel));
			}
			if (chipModel != null && chipModel.length() == "MOSXXXX".length()) {
				// MOS8580 three chars longer than 8580
				col += 3;
			}
			// XXX 3SID currently unsupported
			stereoAddress = detectStereoAddress(ram, videoScreenAddress, row, col + 19);
		}
		return new PSid64DetectedTuneInfo(detected, cpuClock, chipModels, stereoAddress);
	}

	private static String detectChipModel6581or8580(byte[] ram, int videoScreenAddress, int row, int column) {
		for (String chipModelAsString : Arrays.asList("MOS8580", "MOS6581", "8580", "6581")) {
			if (checkScreenMessage(ram, videoScreenAddress, chipModelAsString, row, column)) {
				return chipModelAsString;
			}
		}
		return null;
	}

	private static int detectStereoAddress(byte[] ram, int videoScreenAddress, int row, int column) {
		if (checkScreenMessage(ram, videoScreenAddress, "$", row, column)) {
			return getStereoAdress(ram, videoScreenAddress, row, column);
		}
		return 0;
	}

	private static int getStereoAdress(byte[] ram, int videoScreenAddress, int row, int column) {
		int stereoAddress = getDigit(ram, videoScreenAddress, row, column + 1) << 12
				| getDigit(ram, videoScreenAddress, row, column + 2) << 8
				| getDigit(ram, videoScreenAddress, row, column + 3) << 4
				| getDigit(ram, videoScreenAddress, row, column + 4);
		return stereoAddress;
	}

	private static int getDigit(byte[] ram, int videoScreenAddress, int row, int column) {
		final int offset = (row - 1) * 40 + column - 1;
		int ch = ram[videoScreenAddress + offset] & 0xff;
		if (ch >= 48) {
			// digit
			ch -= 48;
		} else {
			// lower case letter
			ch += 9;
		}
		return ch;
	}

	private static boolean checkScreenMessage(byte[] ram, int videoScreenAddress, String expected, int row,
			int column) {
		final int offset = (row - 1) * 40 + column - 1;
		for (int i = 0; i < expected.length(); i++) {
			final byte screenCode = Petscii.iso88591ToPetscii(expected.charAt(i));
			if (ram[videoScreenAddress + offset + i] != screenCode) {
				return false;
			}
		}
		return true;
	}

	private static ChipModel toChipModel(String chipModelAsString) {
		if (chipModelAsString.equals("MOS8580") || chipModelAsString.equals("8580")) {
			return ChipModel.MOS8580;
		} else {
			return ChipModel.MOS6581;
		}
	}

}
