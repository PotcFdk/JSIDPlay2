/**
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

import java.util.ArrayList;
import java.util.Collection;

import libsidplay.components.pla.PLA;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;

/**
 * An instance of this structure is used to transport values to and from SidTune
 * objects.
 * 
 * @author Ken Händel
 * 
 */
public class SidTuneInfo {
	/**
	 * Load/Init and Play address.
	 */
	protected int loadAddr, initAddr, playAddr;

	/**
	 * Total number of songs contained in a tune.
	 */
	protected int songs;
	
	/**
	 * Start song number.
	 */
	protected int startSong;

	/**
	 * The one song that has been initialized
	 */
	protected int currentSong;

	/**
	 * The SID chip base address for each SID.
	 */
	protected int[] sidChipBase = new int[PLA.MAX_SIDS];

	/**
	 * SID Model for each SID.
	 */
	protected Model[] sidModel = new Model[PLA.MAX_SIDS];

	/**
	 * First available page for relocation
	 */
	protected short relocStartPage;

	/**
	 * Number of pages available for relocation
	 */
	protected short relocPages;

	/**
	 * intended speed, see top
	 */
	protected Clock clockSpeed = Clock.UNKNOWN;
	
	/**
	 * compatibility requirements
	 */
	protected Compatibility compatibility = Compatibility.RSID_BASIC;

	/**
	 * Holds text info from the format headers etc. Song title, credits, ... 0 =
	 * Title, 1 = Author, 2 = Copyright/Publisher
	 */
	protected Collection<String> infoString = new ArrayList<>();

	/**
	 * Used to stash the MUS comment somewhere. MUS comments (2 entries when STR
	 * also has comment)
	 */
	protected Collection<String> commentString = new ArrayList<>();

	/**
	 * length of raw C64 data without load address
	 */
	protected int c64dataLen;

	/**
	 * Calculated driver address for PSID driver (0 if none).
	 */
	protected int determinedDriverAddr;

	/**
	 * Length of driver.
	 */
	protected int determinedDriverLength;

	public SidTuneInfo() {
		this.songs = 1;
		this.startSong = 1;
		this.sidChipBase[0] = 0xd400;
		for (int i = 0; i < sidModel.length; i++) {
			this.sidModel[i] = Model.UNKNOWN;
		}
	}

	public final int getLoadAddr() {
		return loadAddr;
	}

	public final int getInitAddr() {
		return initAddr;
	}

	public final int getPlayAddr() {
		return playAddr;
	}

	public final int getSongs() {
		return songs;
	}

	public final int getStartSong() {
		return startSong;
	}

	public final int getSIDChipBase(int sidNum) {
		return sidChipBase[sidNum];
	}

	public final Model getSIDModel(int sidNum) {
		return sidModel[sidNum];
	}

	public final int getCurrentSong() {
		return currentSong;
	}

	public final Clock getClockSpeed() {
		return clockSpeed;
	}

	public final short getRelocStartPage() {
		return relocStartPage;
	}

	public final short getRelocPages() {
		return relocPages;
	}

	public final Compatibility getCompatibility() {
		return compatibility;
	}

	public final Collection<String> getInfoString() {
		return infoString;
	}

	public final Collection<String> getCommentString() {
		return commentString;
	}

	public final int getC64dataLen() {
		return c64dataLen;
	}

	public final int getDeterminedDriverAddr() {
		return determinedDriverAddr;
	}

	public final int getDeterminedDriverLength() {
		return determinedDriverLength;
	}

	/**
	 * Select sub-song number (null = default starting song).
	 * 
	 * @param song
	 *            The chosen song.
	 */
	public final void setSelectedSong(final Integer song) {
		currentSong = song == null || song > songs ? startSong : song;
	}

	/**
	 * @return The active sub-song number
	 */
	public int getSelectedSong() {
		return currentSong == 0 || currentSong > songs ? startSong : currentSong;
	}

	/**
	 * Temporary hack till real bank switching code added
	 * 
	 * @param addr
	 *            A 16-bit effective address
	 * @return A default bank-select value for $01.
	 */
	public int iomap(final int addr) {
		switch (compatibility) {
		case RSIDv2:
		case RSIDv3:
		case RSID_BASIC:
			return 0; // Special case, converted to 0x37 later
		default:
			if (addr == 0) {
				return 0; // Special case, converted to 0x37 later
			}
			if (addr < 0xa000) {
				return 0x37; // Basic-ROM, Kernal-ROM, I/O
			}
			if (addr < 0xd000) {
				return 0x36; // Kernal-ROM, I/O
			}
			if (addr >= 0xe000) {
				return 0x35; // I/O only
			}
			return 0x34; // RAM only (special I/O in PlaySID mode)
		}
	}

}