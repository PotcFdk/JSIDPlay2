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
package libsidplay.components.sidtune;

import libsidplay.components.sidtune.SidTune.Clock;
import libsidplay.components.sidtune.SidTune.Compatibility;
import libsidplay.components.sidtune.SidTune.Model;
import libsidplay.components.sidtune.SidTune.Speed;

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
	public int loadAddr;

	public int initAddr;

	public int playAddr;

	public int songs;

	public int startSong;

	/**
	 * The SID chip base address used by the sidtune.
	 */
	public int sidChipBase1 = 0xd400;

	/**
	 * The SID chip base address used by the sidtune.
	 * 
	 * 0xD?00 (2nd SID) or 0 (no 2nd SID)
	 */
	public int sidChipBase2;

	//
	// Available after song initialization.
	//

	/**
	 * the one that has been initialized
	 */
	public int currentSong;

	/**
	 * intended speed, see top
	 */
	public Speed songSpeed = Speed.VBI;

	/**
	 * intended speed, see top
	 */
	public Clock clockSpeed = Clock.UNKNOWN;

	/**
	 * First available page for relocation
	 */
	public short relocStartPage;

	/**
	 * Number of pages available for relocation
	 */
	public short relocPages;

	/**
	 * Sid Model required for this sid
	 */
	public Model sidModel = Model.UNKNOWN;

	/**
	 * compatibility requirements
	 */
	public Compatibility compatibility = Compatibility.BASIC;

	/**
	 * Song title, credits, ... 0 = Title, 1 = Author, 2 = Copyright/Publisher
	 * 
	 * the number of available text info lines
	 */
	public short numberOfInfoStrings;

	/**
	 * holds text info from the format headers etc.
	 */
	public String infoString[] = new String[SidTune.SIDTUNE_MAX_CREDIT_STRINGS];

	/**
	 * Number of MUS comments (2 when STR also has comment)
	 */
	public int numberOfCommentStrings;

	/**
	 * Used to stash the MUS comment somewhere. Ignored by everything.
	 */
	public String[] commentString = new String[2];

	/**
	 * length of single-file sidtune file
	 */
	public int dataFileLen;

	/**
	 * length of raw C64 data without load address
	 */
	public int c64dataLen;

	/**
	 * path to sidtune files
	 */
	public String filename;

	/**
	 * Calculated driver address for PSID driver (0 if none).
	 */
	public int determinedDriverAddr;

	/**
	 * Length of driver.
	 */
	public int determinedDriverLength;
}