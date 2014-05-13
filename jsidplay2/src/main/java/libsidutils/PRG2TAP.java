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

public class PRG2TAP {

	static final int MAX_NAME_LENGTH = 16;
	private static final int HEADER_SIZE = 20;

	private static final byte[] C64_TURBO_HEADER = { 0x03, 0x01, 0x08,
			(byte) 0x92, 0x08, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0xd7, 0x05,
			(byte) 0x90, (byte) 0xf0, 0x04, (byte) 0xa9, (byte) 0xff,
			(byte) 0x85, (byte) 0x90, 0x4c, (byte) 0xa9, (byte) 0xf5, 0x20,
			(byte) 0xbf, 0x03, (byte) 0xc9, 0x00, (byte) 0xf0, (byte) 0xf9,
			(byte) 0x85, (byte) 0xab, 0x20, (byte) 0xed, 0x03, (byte) 0x85,
			(byte) 0xc3, 0x20, (byte) 0xed, 0x03, (byte) 0x85, (byte) 0xc4,
			0x20, (byte) 0xed, 0x03, (byte) 0x85, (byte) 0xae, 0x20,
			(byte) 0xed, 0x03, (byte) 0x85, (byte) 0xaf, (byte) 0xa0,
			(byte) 0xbc, 0x20, (byte) 0xed, 0x03, (byte) 0x88, (byte) 0xd0,
			(byte) 0xfa, (byte) 0xf0, 0x2f, 0x20, (byte) 0xbf, 0x03, 0x20,
			(byte) 0xed, (byte) 0x03, (byte) 0x84, (byte) 0x93, 0x48,
			(byte) 0xa9, 0x04, (byte) 0x85, 0x01, 0x68, (byte) 0x91,
			(byte) 0xc3, 0x45, (byte) 0xd7, (byte) 0x85, (byte) 0xd7,
			(byte) 0xa9, 0x07, (byte) 0x85, 0x01, (byte) 0xe6, (byte) 0xc3,
			(byte) 0xd0, 0x02, (byte) 0xe6, (byte) 0xc4, (byte) 0xa5,
			(byte) 0xc3, (byte) 0xc5, (byte) 0xae, (byte) 0xa5, (byte) 0xc4,
			(byte) 0xe5, (byte) 0xaf, (byte) 0x90, (byte) 0xdb, 0x20,
			(byte) 0xed, 0x03, 0x20, 0x02, 0x01, (byte) 0xc8, (byte) 0x84,
			(byte) 0xc0, 0x58, 0x18, (byte) 0xa9, 0x00, (byte) 0x8d,
			(byte) 0xa0, 0x02, 0x4c, (byte) 0x93, (byte) 0xfc, 0x20, 0x17,
			(byte) 0xf8, 0x20, 0x02, 0x01, (byte) 0x84, (byte) 0xd7,
			(byte) 0xa9, 0x07, (byte) 0x8d, 0x06, (byte) 0xdd, (byte) 0xa2,
			0x01, 0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xa5,
			(byte) 0xbd, (byte) 0xc9, 0x02, (byte) 0xd0, (byte) 0xf5,
			(byte) 0xa0, 0x09, 0x20, (byte) 0xed, 0x03, (byte) 0xc9, 0x02,
			(byte) 0xf0, (byte) 0xf9, (byte) 0xc4, (byte) 0xbd, (byte) 0xd0,
			(byte) 0xe8, 0x20, (byte) 0xed, 0x03, (byte) 0x88, (byte) 0xd0,
			(byte) 0xf6, 0x60, (byte) 0xa9, 0x08, (byte) 0x85, (byte) 0xa3,
			0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xee, 0x20,
			(byte) 0xd0, (byte) 0xc6, (byte) 0xa3, (byte) 0xd0 };

	private static final byte[] C64_SLOW_HEADER = { 0x01, 0x01, 0x08,
			(byte) 0x92, 0x08, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab, (byte) 0xab,
			(byte) 0xab, (byte) 0xed, 0x03, (byte) 0x85, (byte) 0xae, 0x20,
			(byte) 0xed, 0x03, (byte) 0x85, (byte) 0xaf, (byte) 0xa0,
			(byte) 0xbc, 0x20, (byte) 0xed, 0x03, (byte) 0x88, (byte) 0xd0,
			(byte) 0xfa, (byte) 0xf0, 0x2f, 0x20, (byte) 0xbf, 0x03, 0x20,
			(byte) 0xed, (byte) 0x03, (byte) 0x84, (byte) 0x93, 0x48,
			(byte) 0xa9, 0x04, (byte) 0x85, 0x01, 0x68, (byte) 0x91,
			(byte) 0xc3, 0x45, (byte) 0xd7, (byte) 0x85, (byte) 0xd7,
			(byte) 0xa9, 0x07, (byte) 0x85, 0x01, (byte) 0xe6, (byte) 0xc3,
			(byte) 0xd0, 0x02, (byte) 0xe6, (byte) 0xc4, (byte) 0xa5,
			(byte) 0xc3, (byte) 0xc5, (byte) 0xae, (byte) 0xa5, (byte) 0xc4,
			(byte) 0xe5, (byte) 0xaf, (byte) 0x90, (byte) 0xdb, 0x20,
			(byte) 0xed, 0x03, 0x20, 0x02, 0x01, (byte) 0xc8, (byte) 0x84,
			(byte) 0xc0, 0x58, 0x18, (byte) 0xa9, 0x00, (byte) 0x8d,
			(byte) 0xa0, 0x02, 0x4c, (byte) 0x93, (byte) 0xfc, 0x20, 0x17,
			(byte) 0xf8, 0x20, 0x02, 0x01, (byte) 0x84, (byte) 0xd7,
			(byte) 0xa9, 0x07, (byte) 0x8d, 0x06, (byte) 0xdd, (byte) 0xa2,
			0x01, 0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xa5,
			(byte) 0xbd, (byte) 0xc9, 0x02, (byte) 0xd0, (byte) 0xf5,
			(byte) 0xa0, 0x09, 0x20, (byte) 0xed, 0x03, (byte) 0xc9, 0x02,
			(byte) 0xf0, (byte) 0xf9, (byte) 0xc4, (byte) 0xbd, (byte) 0xd0,
			(byte) 0xe8, 0x20, (byte) 0xed, 0x03, (byte) 0x88, (byte) 0xd0,
			(byte) 0xf6, 0x60, (byte) 0xa9, 0x08, (byte) 0x85, (byte) 0xa3,
			0x20, 0x16, 0x01, 0x26, (byte) 0xbd, (byte) 0xee, 0x20,
			(byte) 0xd0, (byte) 0xc6, (byte) 0xa3, (byte) 0xd0 };

	private static final byte[] C64_TURBO_DATA = { 0x0b, 0x08, 0x00, 0x00,
			(byte) 0x9e, 0x32, 0x30, 0x36, 0x31, 0x00, 0x00, 0x00, (byte) 0xa2,
			0x05, (byte) 0xbd, (byte) 0x8c, 0x08, (byte) 0x9d, 0x77, 0x02,
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
			(byte) 0xca, (byte) 0xd0, (byte) 0xfd, (byte) 0x88, (byte) 0xd0,
			(byte) 0xfa, 0x78, 0x60, (byte) 0xa9, 0x10, 0x2c, 0x0d,
			(byte) 0xdc, (byte) 0xf0, (byte) 0xfb, (byte) 0xad, 0x0d,
			(byte) 0xdd, (byte) 0x8e, 0x07, (byte) 0xdd, 0x48, (byte) 0xa9,
			0x19, (byte) 0x8d, 0x0f, (byte) 0xdd, 0x68, 0x4a, 0x4a, 0x60,
			(byte) 0x85, (byte) 0x90, 0x20, 0x5d, 0x03, (byte) 0xa5,
			(byte) 0xab, (byte) 0xc9, 0x02, (byte) 0xf0, 0x04, (byte) 0xc9,
			0x01, (byte) 0xd0, (byte) 0xf3, 0x20, (byte) 0x84, 0x03,
			(byte) 0xa5, (byte) 0xbd, 0x45, (byte) 0xf4, (byte) 0xa5,
			(byte) 0xbd, 0x60, 0x4c, (byte) 0xcf, 0x0d, 0x52, (byte) 0xd5, 0x0d };

	private static final int[] PULSE_LENGTH = { 384, 536, 680 };

	private byte tapVersion = 1;
	private int threshold = 263;
	private boolean turboTape = true;

	private BufferedOutputStream out;

	public final void setTapVersion(byte tapVersion) {
		assert (tapVersion != 0 && tapVersion != 1);
		this.tapVersion = tapVersion;
	}

	public final void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	public final void setTurboTape(boolean turboTape) {
		this.turboTape = turboTape;
	}

	/**
	 * Add program to TAP file.
	 */
	public void add(final PRG2TAPProgram program) throws IOException {
		assert (out != null);
		if (turboTape) {
			byte[] header = new byte[C64_TURBO_HEADER.length];
			System.arraycopy(C64_TURBO_HEADER, 0, header, 0,
					C64_TURBO_HEADER.length);
			byte[] data = new byte[C64_TURBO_DATA.length];
			System.arraycopy(C64_TURBO_DATA, 0, data, 0, C64_TURBO_DATA.length);
			for (int i = 0; i < MAX_NAME_LENGTH; i++) {
				header[5 + i] = program.getName()[i];
			}
			header[140] = (byte) (threshold & 255);
			header[145] = (byte) (threshold >> 8);

			slowConvert(header, 0, header.length, 20000);

			addSilence(200000);

			slowConvert(data, 0, data.length, 5000);

			addSilence(1000000);

			turbotapeConvert(program);
		} else {
			byte[] header = new byte[C64_SLOW_HEADER.length];
			System.arraycopy(C64_SLOW_HEADER, 0, header, 0,
					C64_SLOW_HEADER.length);
			header[1] = (byte) (program.getStartAddr() & 0xFF);
			header[2] = (byte) ((program.getStartAddr() >> 8) & 0xFF);
			header[3] = (byte) ((program.getStartAddr() + program.getLength()) & 0xFF);
			header[4] = (byte) (((program.getStartAddr() + program.getLength()) >> 8) & 0xFF);
			for (int i = 0; i < MAX_NAME_LENGTH; i++) {
				header[5 + i] = program.getName()[i];
			}
			slowConvert(header, 0, header.length, 20000);

			addSilence(200000);

			slowConvert(program.getMem(), program.getStartAddr(),
					program.getLength(), 5000);
		}
	}

	/**
	 * Add silence for n cycles
	 * @param ncycles cycles of silence to add (1000000 ~ 1 sec)
	 */
	public void addSilence(final int ncycles) throws IOException {
		if (ncycles < 256 * 8) {
			out.write((byte) (ncycles / 8));
		} else {
			out.write((byte) 0);
			if (tapVersion == 0) {
				return;
			}
			out.write((byte) (ncycles & 0xFF));
			out.write((byte) ((ncycles >> 8) & 0xFF));
			out.write((byte) ((ncycles >> 16) & 0xFF));
		}
	}

	/**
	 * Open TAP file
	 */
	public void open(final File outputFile) throws IOException {
		out = new BufferedOutputStream(new FileOutputStream(outputFile));
		
		byte[] header = "C64-TAPE-RAW".getBytes("ISO-8859-1");
		out.write(header);
		out.write((byte) tapVersion);
		for (int i = 0; i < HEADER_SIZE - header.length; i++) {
			out.write((byte) 0);
		}
		
	}
	
	/**
	 * Close TAP file
	 */
	public void close(File outputFile) throws IOException {
		assert (out != null);
		out.close();
		long size = outputFile.length() - HEADER_SIZE;
		try (RandomAccessFile rnd = new RandomAccessFile(outputFile, "rw")) {
			rnd.seek(HEADER_SIZE - 4);
			rnd.write((byte) (size & 0xFF));
			rnd.write((byte) ((size >> 8) & 0xFF));
			rnd.write((byte) ((size >> 16) & 0xFF));
			rnd.write((byte) ((size >> 24) & 0xFF));
		}
	}

	private void slowConvert(final byte[] data, int startAddr, int length,
			final int leadinLen) throws IOException {
		for (int i = 0; i < leadinLen; i++) {
			addSilence(PULSE_LENGTH[0]);
		}

		for (int i = 137; i > 128; i--) {
			slowWriteByte((byte) i);
		}

		byte checksum = 0;
		for (int i = 0; i < length; i++) {
			slowWriteByte(data[startAddr + i]);
			checksum ^= data[startAddr + i];
		}
		slowWriteByte(checksum);

		addSilence(PULSE_LENGTH[2]);
		addSilence(PULSE_LENGTH[0]);

		for (int i = 0; i < 79; i++) {
			addSilence(PULSE_LENGTH[0]);
		}

		for (int i = 9; i > 0; i--) {
			slowWriteByte((byte) i);
		}
		for (int i = 0; i < length; i++) {
			slowWriteByte(data[startAddr + i]);
		}
		slowWriteByte(checksum);

		addSilence(PULSE_LENGTH[2]);
		addSilence(PULSE_LENGTH[0]);

		for (int i = 0; i < 200; i++) {
			addSilence(PULSE_LENGTH[0]);
		}
	}

	private void slowWriteByte(final byte byt) throws IOException {
		addSilence(PULSE_LENGTH[2]);
		addSilence(PULSE_LENGTH[1]);

		int parity = 1;
		byte count = 1;
		do {
			if ((byt & count) != 0) {
				parity ^= 1;
				addSilence(PULSE_LENGTH[1]);
				addSilence(PULSE_LENGTH[0]);
			} else {
				addSilence(PULSE_LENGTH[0]);
				addSilence(PULSE_LENGTH[1]);
			}
		} while ((count <<= 1) != 0);
		if (parity != 0) {
			addSilence(PULSE_LENGTH[1]);
			addSilence(PULSE_LENGTH[0]);
		} else {
			addSilence(PULSE_LENGTH[0]);
			addSilence(PULSE_LENGTH[1]);
		}
	}

	private void turbotapeConvert(final PRG2TAPProgram program)
			throws IOException {
		for (int i = 0; i < 630; i++) {
			turbotapeWriteByte((byte) 2);
		}
		for (int i = 9; i >= 1; i--) {
			turbotapeWriteByte((byte) i);
		}
		turbotapeWriteByte((byte) 1);
		turbotapeWriteByte((byte) ((program.getStartAddr()) & 0xFF));
		turbotapeWriteByte((byte) ((program.getStartAddr() >> 8) & 0xFF));
		turbotapeWriteByte((byte) ((program.getStartAddr() + program
				.getLength()) & 0xFF));
		turbotapeWriteByte((byte) ((program.getStartAddr()
				+ program.getLength() >> 8) & 0xFF));
		turbotapeWriteByte((byte) 0);
		for (int i = 0; i < MAX_NAME_LENGTH; i++) {
			turbotapeWriteByte(program.getName()[i]);
		}
		for (int i = 0; i < 171; i++) {
			turbotapeWriteByte((byte) 0x20);
		}
		for (int i = 0; i < 630; i++) {
			turbotapeWriteByte((byte) 2);
		}
		for (int i = 9; i >= 1; i--) {
			turbotapeWriteByte((byte) i);
		}
		turbotapeWriteByte((byte) 0);
		byte checksum = 0;
		for (int i = 0; i < program.getLength(); i++) {
			turbotapeWriteByte(program.getMem()[program.getStartAddr() + i]);
			checksum ^= program.getMem()[program.getStartAddr() + i];
		}
		turbotapeWriteByte(checksum);
		for (int i = 0; i < 630; i++) {
			turbotapeWriteByte((byte) 0);
		}
	}

	private void turbotapeWriteByte(final byte byt) throws IOException {
		final int zeroBit = threshold * 4 / 5;
		final int oneBit = threshold * 13 / 10;

		int count = 128;
		do {
			if ((byt & count) != 0) {
				addSilence(oneBit);
			} else {
				addSilence(zeroBit);
			}
		} while ((count = count >> 1) != 0);
	}

}
