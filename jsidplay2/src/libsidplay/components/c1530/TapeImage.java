/*
 * tapeimage.c - Common low-level tape image access.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
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

/**
 * Tape image implementation.
 * 
 * @author Ken Händel
 * 
 */
public class TapeImage {

	/**
	 * Tape instance.
	 */
	private Tap tap;


	/**
	 * Tape image file.
	 */
	private File tapeFile;

	/**
	 * Attach tape image to datasette.
	 * 
	 * @param datasette
	 *            datasette to attach image
	 * @param tapeFile
	 *            tape image file
	 * @return tape image read error
	 * @throws IOException
	 *             cannot read tape image
	 */
	public final boolean imageAttach(final Datasette datasette,
		final File tapeFile) throws IOException {
		// try to open tap file
		final Tap newTap = new Tap();
		if (!newTap.open(tapeFile)) {
			System.err.printf("Cannot open file '%s'\n", tapeFile.getName());
			return false;
		}
		// detach last attached image
		imageDetach(datasette);

		this.tapeFile = tapeFile;
		this.tap = newTap;
		
		// insert tap into datasette
		datasette.setTapeImage(newTap);

		System.out.printf("TAP image '%s' attached.\n", tapeFile.getName());
		System.out.println(String.format(
				"TAP image version: %d, system: %d.", newTap.version,
				newTap.system));
		return true;
	}

	/**
	 * Detach tape image from datasette.
	 * 
	 * @param datasette
	 *            datasette to eject tape
	 * @throws IOException
	 *             tape image read error
	 */
	final void imageDetach(final Datasette datasette) throws IOException {
		if (tap == null || tapeFile == null) {
			// nothing to detach
			return;
		}
		System.out.printf("Detaching TAP image '%s'.\n", tapeFile.getName());
		datasette.setTapeImage(null);
		tap.close();
		this.tapeFile = null;
		this.tap = null;
	}

	public String getName() {
		return tapeFile != null ? tapeFile.getName() : "";
	}

	@Override
	public String toString() {
		return getName();
	}
}
