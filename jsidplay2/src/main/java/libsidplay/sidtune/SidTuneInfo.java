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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;

/**
 * An instance of this structure is used to transport values to and from SidTune
 * objects.<BR>
 * You must read (i.e. activate) sub-song specific information via:
 * 
 * <pre>
 * final SidTuneInfo tuneInfo = SidTune[songNumber];
 * final SidTuneInfo tuneInfo = SidTune.getInfo();
 * void SidTune.getInfo(tuneInfo);
 * </pre>
 * 
 * Consider the following fields as read-only, because the SidTune class does
 * not provide an implementation of:
 * 
 * <pre>
 *  boolean setInfo(final SidTuneInfo)
 * </pre>
 * 
 * Currently, the only way to get the class to accept values which are written
 * to these fields is by creating a derived class.
 * 
 * @author Ken Händel
 * 
 */
public class SidTuneInfo {
	protected int loadAddr;

	protected int initAddr;

	protected int playAddr;

	protected int songs = 1;

	protected int startSong = 1;

	/**
	 * The SID chip base address used by the sidtune.
	 */
	protected int sidChipBase1 = 0xd400;

	/**
	 * The SID chip base address used by the sidtune.
	 * 
	 * 0xD??0 (2nd SID) or 0 (no 2nd SID)
	 */
	protected int sidChipBase2;

	//
	// Available after song initialization.
	//

	/**
	 * the one that has been initialized
	 */
	protected int currentSong;

	/**
	 * intended speed, see top
	 */
	protected Clock clockSpeed = Clock.UNKNOWN;

	/**
	 * First available page for relocation
	 */
	protected short relocStartPage;

	/**
	 * Number of pages available for relocation
	 */
	protected short relocPages;

	/**
	 * Sid Model required for this sid
	 */
	protected Model sid1Model = Model.UNKNOWN;

	/**
	 * Sid Model required for this sid
	 */
	protected Model sid2Model = Model.UNKNOWN;

	/**
	 * compatibility requirements
	 */
	protected Compatibility compatibility = Compatibility.RSID_BASIC;

	/**
	 * Holds text info from the format headers etc.
	 * Song title, credits, ... 0 = Title, 1 = Author, 2 = Copyright/Publisher
	 */
	protected Collection<String> infoString = new ArrayList<>();

	/**
	 * Number of MUS comments (2 when STR also has comment)
	 */
	protected int numberOfCommentStrings;

	/**
	 * Used to stash the MUS comment somewhere. Ignored by everything.
	 */
	protected String[] commentString = new String[2];

	/**
	 * length of raw C64 data without load address
	 */
	protected int c64dataLen;

	/**
	 * path to sidtune files
	 */
	protected File file;

	/**
	 * Calculated driver address for PSID driver (0 if none).
	 */
	protected int determinedDriverAddr;

	/**
	 * Length of driver.
	 */
	protected int determinedDriverLength;

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

	public final int getSidChipBase1() {
		return sidChipBase1;
	}

	public final int getSidChipBase2() {
		return sidChipBase2;
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

	public final Model getSid1Model() {
		return sid1Model;
	}

	public final Model getSid2Model() {
		return sid2Model;
	}

	public final Compatibility getCompatibility() {
		return compatibility;
	}

	public final Collection<String> getInfoString() {
		return infoString;
	}

	public final int getNumberOfCommentStrings() {
		return numberOfCommentStrings;
	}

	public final String[] getCommentString() {
		return commentString;
	}

	public final int getC64dataLen() {
		return c64dataLen;
	}

	public final File getFile() {
		return file;
	}

	public final int getDeterminedDriverAddr() {
		return determinedDriverAddr;
	}

	public final int getDeterminedDriverLength() {
		return determinedDriverLength;
	}

}