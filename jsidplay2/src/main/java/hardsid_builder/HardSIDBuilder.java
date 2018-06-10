package hardsid_builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;

/**
 * <pre>
 * **************************************************************************
 *       hardsid-builder.cpp - HardSID builder class for creating/controlling
 *                             HardSIDs.
 *                             -------------------
 *  begin                : Wed Sep 5 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 * **************************************************************************
 * **************************************************************************
 * * This program is free software; you can redistribute it and/or modify 
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation; either version 2 of the License, or
 * * (at your option) any later version.
 * **************************************************************************
 * </pre>
 * 
 * @author Ken Händel
 * 
 */
public class HardSIDBuilder implements SIDBuilder {

	private static final short REGULAR_DELAY = 256;

	static final short SHORTEST_DELAY = 4;

	/**
	 * System event context.
	 */
	private EventScheduler context;

	/**
	 * Configuration
	 */
	private IConfig config;

	/**
	 * Native library wrapper.
	 */
	private static HardSID4U hardSID;

	/**
	 * Already used HardSIDs.
	 */
	private List<HardSID> sids = new ArrayList<HardSID>();

	/**
	 * Device number, if more than one USB devices is connected.
	 */
	private byte deviceID;

	protected long lastSIDWriteTime;

	private static boolean initialized;

	public HardSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		if (!initialized) {
			// Extract and Load JNI driver wrapper recognizing real HardSID4U devices
			try {
				System.load(extract(config, "/hardsid_builder/win32/Release/", "JHardSID.dll"));
				initialized = true;
			} catch (Error e) {
				System.err.println("32-bit Java and Windows is required to make use of that feature!");
				throw e;
			} catch (IOException e) {
				throw new RuntimeException(String.format("HARDSID ERROR: JHardSID.dll not found!"));
			}
		}
		// Go and use JNI driver wrapper
		hardSID = new HardSID4U();
	}

	/**
	 * Extract a classpath resource referencing a file to the temp directory and
	 * mark to be deleted after exit.
	 * 
	 * @param config
	 *            configuration
	 * @param path
	 *            resource path
	 * @param libName
	 *            filename
	 * @return absolute path name of the extracted file
	 * @throws IOException
	 *             I/O error
	 */
	private String extract(final IConfig config, final String path, final String libName) throws IOException {
		File file = new File(new File(config.getSidplay2Section().getTmpDir()), libName);
		file.deleteOnExit();
		try (InputStream is = getClass().getResourceAsStream(path + libName)) {
			Files.copy(is, Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
		}
		return file.getAbsolutePath();
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		final ChipModel chipModel = getChipModel(tune, sidNum);
		final byte chipNum = getModelDependantSidNum(chipModel, sidNum);
		if (deviceID < hardSID.HardSID_DeviceCount() && chipNum < hardSID.HardSID_SIDCount(deviceID)) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			HardSID hsid = new HardSID(context, this, hardSID, deviceID, chipNum, chipModel);
			sids.add(hsid);
			hsid.lock();
			return hsid;
		}
		throw new RuntimeException(
				String.format("HARDSID ERROR: System doesn't have enough SID chips. Requested: (DeviceID=%d, SID=%d)",
						deviceID, chipNum));
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		HardSID hardSid = (HardSID) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	/**
	 * Choose desired chip model.
	 * <OL>
	 * <LI>Detect chip model of specific SID number
	 * <LI>For the second SID (stereo) always use the other model
	 * </OL>
	 * Note: In mono mode we always want to use a SID depending on the correct
	 * chip model. But, in stereo mode we need another SID. Therefore we change
	 * the chip model to match the second configured SID.
	 * 
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * @return desired chip model
	 */
	private ChipModel getChipModel(SidTune tune, int sidNum) {
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		if (sids.size() > 0) {
			// Stereo SID? Use a HardSID SID different to the first SID
			ChipModel modelAlreadyInUse = sids.get(0).getChipModel();
			if (chipModel == modelAlreadyInUse) {
				chipModel = (chipModel == ChipModel.MOS6581) ? ChipModel.MOS8580 : ChipModel.MOS6581;
			}
		}
		return chipModel;
	}

	/**
	 * Get SID index based on the desired chip model.
	 * 
	 * @param chipModel
	 *            desired chip model
	 * @param sidNum
	 *            current SID number
	 * @return SID index of the desired HardSID SID
	 */
	private byte getModelDependantSidNum(final ChipModel chipModel, int sidNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();
		if (sidNum == 2) {
			// 3-SID: for now choose next available free slot
			for (byte i = 0; i < hardSID.HardSID_SIDCount(deviceID); i++) {
				if (i != sid6581 && i != sid8580) {
					System.err.println("Use 1st:" + sid6581 + ", 2nd:" + sid8580 + ", 3rd:" + i);
					return i;
				}
			}
			throw new RuntimeException(String.format(
					"HARDSID ERROR: System doesn't have enough SID chips. Requested: (DeviceID=%d, SIDs=%d)", deviceID,
					hardSID.HardSID_SIDCount(deviceID)));
		}
		return (byte) (chipModel == ChipModel.MOS6581 ? sid6581 : sid8580);
	}

	int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return Math.max(SHORTEST_DELAY, diff);
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY << 1) {
			lastSIDWriteTime += REGULAR_DELAY;
			hardSID.HardSID_Delay(deviceID, REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}

}
