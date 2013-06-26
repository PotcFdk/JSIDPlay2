/*
 * tape.c - Tape unit emulation.
 *
 * Written by
 *  Ettore Perazzoli <ettore@comm2000.it>
 *  Andreas Boose <viceteam@t-online.de>
 *
 * Based on older code by
 *  Jouko Valta <jopi@stekt.oulu.fi>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.c1530;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * TAP filetype implementation.
 * 
 * @author Ken Händel
 * 
 */
public class Tap {
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");
	
	/**
	 * Size of the TAP file header.
	 */
	public static final int TAP_HDR_SIZE = 20;
	/**
	 * Offset of the header magic string.
	 */
	public static final int TAP_HDR_MAGIC_OFFSET = 0;
	/**
	 * Offset of the TAP header version info.
	 */
	public static final int TAP_HDR_VERSION = 12;
	/**
	 * Offset of the TAP header system info.
	 */
	public static final int TAP_HDR_SYSTEM = 13;
	/**
	 * Length of the TAP file header infos.
	 */
	public static final int TAP_HDR_LEN = 16;

	/**
	 * TAP file descriptor.
	 */
	RandomAccessFile fd;

	/**
	 * Size of the TAP image.
	 */
	long size;

	/**
	 * The TAP version byte.
	 */
	byte version;

	/**
	 * System the image is made for.
	 */
	byte system;

	/**
	 * Position in the current file.
	 */
	long currentFilePosition;

	/**
	 * Header offset.
	 */
	int offset;

	/**
	 * Tape counter in machine-cycles/8 for even looong tapes.
	 */
	int cycleCounter;

	/**
	 * Tape length in machine-cycles/8.
	 */
	int cycleCounterTotal;

	/**
	 * Tape counter.
	 */
	int counter;

	/**
	 * Read only tape?
	 */
	private boolean readOnly;

	/**
	 * @return Is this tape read only?
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
	
	/**
	 * Has the tap changed? We correct the size then.
	 */
	boolean hasChanged;

	/**
	 * Constructor.
	 */
	public Tap() {
		offset = Tap.TAP_HDR_SIZE;
	}

	/**
	 * Read TAP header.
	 * 
	 * @return Is the file descriptor a valid TAP file?
	 */
	public final boolean readHeader() {
		byte[] buf = new byte[TAP_HDR_SIZE];

		try {
			fd.readFully(buf);
		} catch (IOException e) {
			return false;
		}

		String checkName = new String(buf, TAP_HDR_MAGIC_OFFSET, 12, ISO88591);
		if (!checkName.startsWith("C64-TAPE-RAW")
				&& !checkName.startsWith("C16-TAPE-RAW")) {
			return false;
		}
		version = buf[TAP_HDR_VERSION];
		system = buf[TAP_HDR_SYSTEM];

		return true;
	}

	/**
	 * Open tape image.
	 * 
	 * @param tapeFile
	 *            tape file to open
	 * @return Is this image a TAP image file?
	 * @throws IOException
	 *             tape image read error
	 */
	public final boolean open(final File tapeFile) throws IOException {
		if (!readOnly) {
			try {
				fd = new RandomAccessFile(tapeFile, "rw");
				readOnly = false;
			} catch (IOException e) {
				fd = new RandomAccessFile(tapeFile, "r");
				readOnly = true;
			}
		}

		if (!readHeader()) {
			fd.close();
			return false;
		}

		size = fd.length() - Tap.TAP_HDR_SIZE;

		if (size < 3) {
			fd.close();
			return false;
		}

		return true;
	}

	/**
	 * Close tape image.
	 * 
	 * @throws IOException
	 *             tape image read error
	 */
	public final void close() throws IOException {
		if (fd != null) {
			if (hasChanged) {
				fd.seek(TAP_HDR_LEN);
				writeFilesize(new int[] { (int) size });
			}
			fd.close();
			fd = null;
		}
	}

	/**
	 * Write a filesize to the tape image, if the image contents has changed.
	 * 
	 * @param buf
	 *            array containing one int with the file size
	 * @throws IOException
	 *             tape image write error
	 */
	private void writeFilesize(final int[] buf) throws IOException {
		ByteBuffer b = ByteBuffer.wrap(new byte[1 << 2]).order(
				ByteOrder.LITTLE_ENDIAN);
		b.asIntBuffer().put(buf, 0, 1);
		b.rewind();
		fd.write(b.array(), 0, 1 << 2);
	}

	/**
	 * Got to the start position of the tape image.
	 * 
	 * @throws IOException tape image write error
	 */
	final void seekStart() throws IOException {
		currentFilePosition = 0;
		fd.seek(offset);
	}

}
