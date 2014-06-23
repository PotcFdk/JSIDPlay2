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
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

import libsidutils.assembler.KickAssembler;

public class PRG2TAP {

	private static final String TURBO_HEADER_ASM = "/libsidutils/PRG2TAP_TurboHeader.asm";
	private static final String TURBO_DATA_ASM = "/libsidutils/PRG2TAP_TurboData.asm";
	private static final String SLOW_HEADER_ASM = "/libsidutils/PRG2TAP_SlowHeader.asm";

	static final int MAX_NAME_LENGTH = 16;
	private static final int TAP_HEADER_SIZE = 20;

	private static final int[] PULSE_LENGTH = { 384, 536, 680 };

	private byte tapVersion = 1;
	private int threshold = 263;
	private boolean turboTape = true;

	private final KickAssembler assembler = new KickAssembler();
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
		HashMap<String, String> globals;
		InputStream asm;
		if (turboTape) {
			globals = new HashMap<String, String>();
			globals.put("name", getName(program));
			globals.put("threshold", String.valueOf(threshold));
			asm = PRG2TAP.class.getResourceAsStream(TURBO_HEADER_ASM);
			byte[] header = assembler.assemble(TURBO_HEADER_ASM, asm, globals);

			globals = new HashMap<String, String>();
			asm = PRG2TAP.class.getResourceAsStream(TURBO_DATA_ASM);
			byte[] data = assembler.assemble(TURBO_DATA_ASM, asm, globals);

			slowConvert(header, 2, header.length - 2, 20000);
			addSilence(200000);
			slowConvert(data, 2, data.length - 2, 5000);
			addSilence(1000000);
			turbotapeConvert(program);
		} else {
			final byte[] data = program.getMem();
			final int start = program.getStartAddr();
			final int end = start + program.getLength();

			globals = new HashMap<String, String>();
			globals.put("name", getName(program));
			globals.put("start", String.valueOf(start));
			globals.put("end", String.valueOf(end));
			asm = PRG2TAP.class.getResourceAsStream(SLOW_HEADER_ASM);
			byte[] header = assembler.assemble(SLOW_HEADER_ASM, asm, globals);

			slowConvert(header, 2, header.length - 2, 20000);
			addSilence(200000);
			slowConvert(data, start, end - start, 5000);
		}
	}

	private String getName(final PRG2TAPProgram program) {
		String name = "";
		for (int i = 0; i < MAX_NAME_LENGTH; i++) {
			name += (char) program.getName()[i];
		}
		return name;
	}

	/**
	 * Add silence for n cycles
	 * 
	 * @param ncycles
	 *            cycles of silence to add (1000000 ~ 1 sec)
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
		for (int i = 0; i < TAP_HEADER_SIZE - header.length; i++) {
			out.write((byte) 0);
		}

	}

	/**
	 * Close TAP file
	 */
	public void close(File outputFile) throws IOException {
		assert (out != null);
		out.close();
		long size = outputFile.length() - TAP_HEADER_SIZE;
		try (RandomAccessFile rnd = new RandomAccessFile(outputFile, "rw")) {
			rnd.seek(TAP_HEADER_SIZE - 4);
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
