/* WAV-PRG: a program for converting C64 tapes into files suitable
 * for emulators and back.
 *
 * Copyright (c) Fabrizio Gennari, 1998-2003
 *
 * The program is distributed under the GNU General Public License.
 * See file LICENSE.TXT for details.
 *
 * prg2wav.c : the main function of prg2wav, parses options and calls
 * the core processing function with the right arguments
 * This file belongs to the prg->wav part
 * This file is part of the command-line version of WAV-PRG
 */
package libsidutils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import libsidplay.components.DirEntry;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.T64;

public class PRG2TAP {

	protected static class TapHandle {
		File fileHandle;
		BufferedOutputStream file;
		byte version;
	}

	protected static class C64Program {
		byte[] name = new byte[16];
		int startAddr;
		int endAddr;
		byte[] data = new byte[65536];
	}

	private final boolean main2(final String[] argo) {
		String inputFilename = argo[0];
		String outputFilename = argo[1];

		byte tapVersion = 1;
		int threshold = 263;

		// load SidTune
		C64Program program = new C64Program();
		try {
			SidTune p = SidTune.load(new File(inputFilename));
			if (p == null) {
				return false;
			}
			p.placeProgramInMemory(program.data);
			program.startAddr = p.getInfo().loadAddr;
			program.endAddr = program.startAddr + p.getInfo().c64dataLen;
			// tape filename to be used:
			final byte[] petsciiName;
			if (p instanceof T64) {
				petsciiName = ((T64) p).getLastEntryName();
			} else {
				String filenameNoExt = new File(inputFilename).getName();
				if (filenameNoExt.lastIndexOf('.') != -1) {
					filenameNoExt = filenameNoExt.substring(0,
							filenameNoExt.lastIndexOf('.'));
				}
				petsciiName = DirEntry.asciiTopetscii(filenameNoExt, 16);
			}
			System.arraycopy(petsciiName, 0, program.name, 0, Math.min(16,
					petsciiName.length));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SidTuneError e) {
			e.printStackTrace();
		}

		TapHandle handle = new TapHandle();
		if (tapfileInitWrite(outputFilename, handle, tapVersion) != 0) {
			System.out.printf("Error creating TAP file %s\n", outputFilename);
			return false;
		}

		try {
			convertFromPrg(program, handle, threshold);

			// TODO add several files?
			// pause: prg2wav_set_pulse(params.file, 1000000); /* 1 sec silence */
			// next file: convert_from_prg(program, handle, threshold);

			tapfileWriteClose(handle);
		} catch (IOException e) {
			System.err.printf("Error writing tap: error %s\n", e.getMessage());
		}
		return true;
	}

	private void convertFromPrg(final C64Program program,
			final TapHandle handle, final int threshold) throws IOException {
		byte[] c64HeaderChunk = { 0x03, 0x01, 0x08, (byte) 0x92, 0x08, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0xd7, 0x05, (byte) 0x90,
				(byte) 0xf0, 0x04, (byte) 0xa9, (byte) 0xff, (byte) 0x85,
				(byte) 0x90, 0x4c, (byte) 0xa9, (byte) 0xf5, 0x20, (byte) 0xbf,
				0x03, (byte) 0xc9, 0x00, (byte) 0xf0, (byte) 0xf9, (byte) 0x85,
				(byte) 0xab, 0x20, (byte) 0xed, 0x03, (byte) 0x85, (byte) 0xc3,
				0x20, (byte) 0xed, 0x03, (byte) 0x85, (byte) 0xc4, 0x20,
				(byte) 0xed, 0x03, (byte) 0x85, (byte) 0xae, 0x20, (byte) 0xed,
				0x03, (byte) 0x85, (byte) 0xaf, (byte) 0xa0, (byte) 0xbc, 0x20,
				(byte) 0xed, 0x03, (byte) 0x88, (byte) 0xd0, (byte) 0xfa,
				(byte) 0xf0, 0x2f, 0x20, (byte) 0xbf, 0x03, 0x20, (byte) 0xed,
				(byte) 0x03, (byte) 0x84, (byte) 0x93, 0x48, (byte) 0xa9, 0x04,
				(byte) 0x85, 0x01, 0x68, (byte) 0x91, (byte) 0xc3, 0x45,
				(byte) 0xd7, (byte) 0x85, (byte) 0xd7, (byte) 0xa9, 0x07,
				(byte) 0x85, 0x01, (byte) 0xe6, (byte) 0xc3, (byte) 0xd0, 0x02,
				(byte) 0xe6, (byte) 0xc4, (byte) 0xa5, (byte) 0xc3,
				(byte) 0xc5, (byte) 0xae, (byte) 0xa5, (byte) 0xc4,
				(byte) 0xe5, (byte) 0xaf, (byte) 0x90, (byte) 0xdb, 0x20,
				(byte) 0xed, 0x03, 0x20, 0x02, 0x01, (byte) 0xc8, (byte) 0x84,
				(byte) 0xc0, 0x58, 0x18, (byte) 0xa9, 0x00, (byte) 0x8d,
				(byte) 0xa0, 0x02, 0x4c, (byte) 0x93, (byte) 0xfc, 0x20, 0x17,
				(byte) 0xf8, 0x20, 0x02, 0x01, (byte) 0x84, (byte) 0xd7,
				(byte) 0xa9, 0x07, (byte) 0x8d, 0x06, (byte) 0xdd, (byte) 0xa2,
				0x01, 0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xa5,
				(byte) 0xbd, (byte) 0xc9, 0x02, (byte) 0xd0, (byte) 0xf5,
				(byte) 0xa0, 0x09, 0x20, (byte) 0xed, 0x03, (byte) 0xc9, 0x02,
				(byte) 0xf0, (byte) 0xf9, (byte) 0xc4, (byte) 0xbd,
				(byte) 0xd0, (byte) 0xe8, 0x20, (byte) 0xed, 0x03, (byte) 0x88,
				(byte) 0xd0, (byte) 0xf6, 0x60, (byte) 0xa9, 0x08, (byte) 0x85,
				(byte) 0xa3, 0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xee,
				0x20, (byte) 0xd0, (byte) 0xc6, (byte) 0xa3, (byte) 0xd0 };
		final byte[] c64DataChunk = { 0x0b, 0x08, 0x00, 0x00, (byte) 0x9e,
				0x32, 0x30, 0x36, 0x31, 0x00, 0x00, 0x00, (byte) 0xa2, 0x05,
				(byte) 0xbd, (byte) 0x8c, 0x08, (byte) 0x9d, 0x77, 0x02,
				(byte) 0xca, 0x10, (byte) 0xf7, (byte) 0xa9, 0x06, (byte) 0x85,
				(byte) 0xc6, (byte) 0xa9, 0x03, (byte) 0x8d, 0x31, 0x03,
				(byte) 0xa9, 0x3c, (byte) 0x8d, 0x30, 0x03, (byte) 0xa2, 0x2a,
				(byte) 0xbd, 0x48, 0x08, (byte) 0x9d, 0x02, 0x01, (byte) 0xca,
				0x10, (byte) 0xf7, (byte) 0xa2, 0x15, (byte) 0xbd, 0x72, 0x08,
				(byte) 0x9d, 0x3b, 0x03, (byte) 0xca, (byte) 0xd0, (byte) 0xf7,
				(byte) 0xa2, 0x04, (byte) 0xbd, (byte) 0x87, 0x08, (byte) 0x9d,
				(byte) 0xfb, 0x03, (byte) 0xca, (byte) 0xd0, (byte) 0xf7, 0x60,
				(byte) 0xa0, 0x00, (byte) 0x84, (byte) 0xc0, (byte) 0xad, 0x11,
				(byte) 0xd0, 0x29, (byte) 0xef, (byte) 0x8d, 0x11, (byte) 0xd0,
				(byte) 0xca, (byte) 0xd0, (byte) 0xfd, (byte) 0x88,
				(byte) 0xd0, (byte) 0xfa, 0x78, 0x60, (byte) 0xa9, 0x10, 0x2c,
				0x0d, (byte) 0xdc, (byte) 0xf0, (byte) 0xfb, (byte) 0xad, 0x0d,
				(byte) 0xdd, (byte) 0x8e, 0x07, (byte) 0xdd, 0x48, (byte) 0xa9,
				0x19, (byte) 0x8d, 0x0f, (byte) 0xdd, 0x68, 0x4a, 0x4a, 0x60,
				(byte) 0x85, (byte) 0x90, 0x20, 0x5d, 0x03, (byte) 0xa5,
				(byte) 0xab, (byte) 0xc9, 0x02, (byte) 0xf0, 0x04, (byte) 0xc9,
				0x01, (byte) 0xd0, (byte) 0xf3, 0x20, (byte) 0x84, 0x03,
				(byte) 0xa5, (byte) 0xbd, 0x45, (byte) 0xf4, (byte) 0xa5,
				(byte) 0xbd, 0x60, 0x4c, (byte) 0xcf, 0x0d, 0x52, (byte) 0xd5,
				0x0d };
		int namePos = 5;

		if (program.endAddr < program.startAddr) {
			System.err.printf("End address lower than start address\n");
			return;
		}

		for (int i = 0; i < 16; i++) {
			c64HeaderChunk[namePos + i] = program.name[i];
		}
		c64HeaderChunk[140] = (byte) (threshold & 255);
		c64HeaderChunk[145] = (byte) (threshold >> 8);
		if (slowConvert(handle, c64HeaderChunk, 0, c64HeaderChunk.length, 20000) != 0) {
			return;
		}
		tapfileWriteSetPulse(handle, 200000);
		if (slowConvert(handle, c64DataChunk, 0, c64DataChunk.length, 5000) != 0) {
			return;
		}
		tapfileWriteSetPulse(handle, 1000000);
		if (turbotapeConvert(handle, program, threshold) != 0) {
			return;
		}
	}

	private void tapfileWriteSetPulse(final TapHandle handle, final int ncycles)
			throws IOException {
		byte[] threebytes = new byte[3];
		byte byt;

		if (ncycles < 256 * 8) {
			byt = (byte) (ncycles / 8);
			handle.file.write(byt);
			return;
		}
		byt = 0;
		handle.file.write(byt);

		if (handle.version == 0) {
			return;
		}
		threebytes[0] = (byte) (ncycles & 0xFF);
		threebytes[1] = (byte) ((ncycles >> 8) & 0xFF);
		threebytes[2] = (byte) ((ncycles >> 16) & 0xFF);
		handle.file.write(threebytes);
	}

	private void tapfileWriteClose(final TapHandle handle) throws IOException {
		handle.file.close();

		long size = handle.fileHandle.length();

		size -= 20;
		if (size < 0) {
			System.err.printf("Invalid file size\n");
			return;
		}

		byte[] sizeHeader = new byte[4];
		sizeHeader[0] = (byte) (size & 0xFF);
		sizeHeader[1] = (byte) ((size >> 8) & 0xFF);
		sizeHeader[2] = (byte) ((size >> 16) & 0xFF);
		sizeHeader[3] = (byte) ((size >> 24) & 0xFF);
		RandomAccessFile rnd = null;
		try {
			rnd = new RandomAccessFile(handle.fileHandle, "rw");
			rnd.seek(16);
			rnd.write(sizeHeader);
		} catch (IOException e) {
			System.err
					.printf(
							"Cannot write to file, header won't have correct size: error %s\n",
							e.getMessage());
		} finally {
			if (rnd != null) {
				try {
					rnd.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	private int tapfileInitWrite(final String name, final TapHandle handle,
			final byte version) {
		if (version != 0 && version != 1) {
			System.err.printf("Unsupported version %d\n", version);
			return -1;
		}

		try {
			handle.fileHandle = new File(name);
			handle.file = new BufferedOutputStream(new FileOutputStream(
					handle.fileHandle));
		} catch (IOException e) {
			System.err
					.printf("Could not open file: error %s\n", e.getMessage());
			return -1;
		}

		try {
			byte[] c64TapHeader = "C64-TAPE-RAW\0\0\0\0\0\0\0\0"
					.getBytes("ISO-8859-1");
			c64TapHeader[12] = version;
			handle.file.write(c64TapHeader);
		} catch (IOException e) {
			System.err.printf("Could not write to file: error %s\n", e
					.getMessage());
			try {
				handle.file.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return -1;
		}

		handle.version = version;

		return 0;
	}

	private static final int[] PULSE_LENGTH = { 384, 536, 680 }; /* VIC20/C64 */

	private int slowConvert(final TapHandle handle, final byte[] data,
			final int dataPos, final int size, final int leadin_len)
			throws IOException {
		byte checksum = 0;

		for (int num = 0; num < leadin_len; num++) {
			tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
		}

		for (int i = 137; i > 128; i--) {
			if (slowWriteByte(handle, (byte) i) != 0)
				return -1;
		}
		for (int num = 0; num < size; num++) {
			if (slowWriteByte(handle, data[dataPos + num]) != 0) {
				return -1;
			}
			checksum ^= data[dataPos + num];
		}
		if (slowWriteByte(handle, checksum) != 0) {
			return -1;
		}

		/*
		 * C64 and VIC20 files end with long-short
		 */
		tapfileWriteSetPulse(handle, PULSE_LENGTH[2]);
		tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);

		for (int num = 0; num < 79; num++) {
			tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
		}

		for (int i = 9; i > 0; i--) {
			if (slowWriteByte(handle, (byte) i) != 0)
				return -1;
		}
		for (int num = 0; num < size; num++) {
			if (slowWriteByte(handle, data[dataPos + num]) != 0) {
				return -1;
			}
		}
		if (slowWriteByte(handle, checksum) != 0) {
			return -1;
		}

		tapfileWriteSetPulse(handle, PULSE_LENGTH[2]);
		tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);

		for (int num = 0; num < 200; num++) {
			tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
		}

		return 0;
	}

	private int slowWriteByte(final TapHandle handle, final byte byt)
			throws IOException {
		byte count = 1;
		int parity = 1;

		tapfileWriteSetPulse(handle, PULSE_LENGTH[2]);
		tapfileWriteSetPulse(handle, PULSE_LENGTH[1]);

		do {
			if ((byt & count) != 0) {
				parity ^= 1;
				tapfileWriteSetPulse(handle, PULSE_LENGTH[1]);
				tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
			} else {
				tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
				tapfileWriteSetPulse(handle, PULSE_LENGTH[1]);
			}
		} while ((count <<= 1) != 0);
		if (parity != 0) {
			tapfileWriteSetPulse(handle, PULSE_LENGTH[1]);
			tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
		} else {
			tapfileWriteSetPulse(handle, PULSE_LENGTH[0]);
			tapfileWriteSetPulse(handle, PULSE_LENGTH[1]);
		}

		return 0;
	}

	private int turbotapeConvert(final TapHandle handle,
			final C64Program program, final int threshold) throws IOException {
		int i;
		byte checksum = 0;
		byte byt;

		for (i = 0; i < 630; i++) {
			if (turbotapeWriteByte(handle, (byte) 2, threshold) != 0)
				return -1;
		}
		for (byt = 9; byt >= 1; byt--) {
			if (turbotapeWriteByte(handle, byt, threshold) != 0)
				return -1;
		}
		if (turbotapeWriteByte(handle, (byte) 1, threshold) != 0) {
			return -1;
		}
		if (turbotapeWriteByte(handle, (byte) ((program.startAddr) & 0xFF),
				threshold) != 0) {
			return -1;
		}
		if (turbotapeWriteByte(handle,
				(byte) ((program.startAddr >> 8) & 0xFF), threshold) != 0) {
			return -1;
		}
		if (turbotapeWriteByte(handle, (byte) ((program.endAddr) & 0xFF),
				threshold) != 0) {
			return -1;
		}
		if (turbotapeWriteByte(handle, (byte) ((program.endAddr >> 8) & 0xFF),
				threshold) != 0) {
			return -1;
		}
		if (turbotapeWriteByte(handle, (byte) 0, threshold) != 0) {
			return -1;
		}
		for (i = 0; i < 16; i++) {
			if (turbotapeWriteByte(handle, program.name[i], threshold) != 0)
				return -1;
		}
		for (i = 0; i < 171; i++) {
			if (turbotapeWriteByte(handle, (byte) 0x20, threshold) != 0)
				return -1;
		}
		for (i = 0; i < 630; i++) {
			if (turbotapeWriteByte(handle, (byte) 2, threshold) != 0)
				return -1;
		}
		for (byt = 9; byt >= 1; byt--) {
			if (turbotapeWriteByte(handle, byt, threshold) != 0)
				return -1;
		}
		if (turbotapeWriteByte(handle, (byte) 0, threshold) != 0) {
			return -1;
		}
		for (i = program.startAddr; i < program.endAddr; i++) {
			if (turbotapeWriteByte(handle, program.data[i], threshold) != 0) {
				return -1;
			}
			checksum ^= program.data[i];
		}
		if (turbotapeWriteByte(handle, checksum, threshold) != 0) {
			return -1;
		}
		for (i = 0; i < 630; i++) {
			if (turbotapeWriteByte(handle, (byte) 0, threshold) != 0)
				return -1;
		}

		return 0;
	}

	private int turbotapeWriteByte(final TapHandle handle, final byte byt,
			final int threshold) throws IOException {
		int count = 128;
		int zeroBit = threshold * 4 / 5;
		int oneBit = threshold * 13 / 10;

		do {
			if ((byt & count) != 0) {
				tapfileWriteSetPulse(handle, oneBit);
			} else {
				tapfileWriteSetPulse(handle, zeroBit);
			}
		} while ((count = count >> 1) != 0);

		return 0;
	}

	public static final void main(final String[] args) {
		new PRG2TAP().main2(args);
	}
}
