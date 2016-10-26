package hardsid_builder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import libsidplay.common.ChipModel;
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

	private static final int HSID_VERSION_MIN = 0x0207;

	/**
	 * Output buffer size.
	 */
	public static final int MAX_BUFFER_SIZE = 1 << 20;

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
	private static HsidDLL2 hsidDLL;

	/**
	 * Already used HardSIDs.
	 */
	private List<HardSID> sids = new ArrayList<HardSID>();

	/**
	 * @param config
	 *            configuration
	 */
	public HardSIDBuilder(EventScheduler context, IConfig config) {
		this.context = context;
		this.config = config;
		// Extract original HardSID4U driver, loaded by JNI driver wrapper below
		try {
			extract(config, "/hardsid_builder/win32/Release/", "HardSID.dll");
		} catch (IOException e) {
			throw new RuntimeException(String.format("HARDSID ERROR: HardSID_orig.dll not found"));
		}
		// Extract and Load JNI driver wrapper recognizing netsiddev devices and real devices
		try {
			System.load(extract(config, "/hardsid_builder/win32/Release/", "JHardSID.dll"));
		} catch (IOException e) {
			throw new RuntimeException(String.format("HARDSID ERROR: JHardSID.dll not found!"));
		}
		// Go and use JNI driver wrapper
		hsidDLL = new HsidDLL2();
		// JNI driver wrapper loads the HardSID driver
		hsidDLL.InitHardSID_Mapper();
		if (hsidDLL.HardSID_Version() < HSID_VERSION_MIN) {
			throw new RuntimeException("HARDSID ERROR: HardSID4U version 2.07 is at least required!");
		}
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		final ChipModel chipModel = getChipModel(tune, sidNum);
		final int deviceIdx = getModelDependantDevice(chipModel, sidNum);
		if (deviceIdx < hsidDLL.HardSID_Devices()) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			HardSID hsid = new HardSID(this, context, hsidDLL, deviceIdx, chipModel);
			hsid.lock();
			sids.add(hsid);
			return hsid;
		}
		throw new RuntimeException("HARDSID ERROR: System doesn't have enough SID chips.");
	}

	@Override
	public void unlock(final SIDEmu device) {
		HardSID hardSid = (HardSID) device;
		hardSid.unlock();
		sids.remove(device);
	}

	public int getSIDCount() {
		return sids.size();
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
		File f = new File(new File(config.getSidplay2Section().getTmpDir()), libName);
		if (!f.exists()) {
			f.deleteOnExit();
			writeResource(path + libName, f);
		}
		return f.getAbsolutePath();
	}

	/**
	 * Write class path resource to a file.
	 * 
	 * @param resource
	 *            class path resource
	 * @param file
	 *            output file
	 * @throws IOException
	 *             I/O error
	 */
	private void writeResource(final String resource, final File file) throws IOException {
		try (InputStream is = getClass().getResourceAsStream(resource);
				OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
			int bytesRead;
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		}
	}

	/**
	 * Choose desired chip model.
	 * <OL>
	 * <LI>Detect chip model of specific SID number
	 * <LI>For the second device (stereo) always use the other model
	 * </OL>
	 * Note: In mono mode we always want to use a device depending on the
	 * correct chip model. But, in stereo mode we need another device. Therefore
	 * we change the chip model to match the second configured device.
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
			// Stereo device? Use a HardSID device different to the first SID
			ChipModel modelAlreadyInUse = sids.get(0).getChipModel();
			if (chipModel == modelAlreadyInUse) {
				chipModel = (chipModel == ChipModel.MOS6581) ? ChipModel.MOS8580 : ChipModel.MOS6581;
			}
		}
		return chipModel;
	}

	/**
	 * Get device index based on the desired chip model.
	 * 
	 * @param chipModel
	 *            desired chip model
	 * @param sidNum
	 *            current SID number
	 * @return device index of the desired HardSID device
	 */
	private int getModelDependantDevice(final ChipModel chipModel, int sidNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();
		if (sidNum == 2) {
			// 3-SID: for now choose next available free slot
			for (int i = 0; i < getSIDCount(); i++) {
				if (i != sid6581 && i != sid8580) {
					return i;
				}
			}
		}
		return chipModel == ChipModel.MOS6581 ? sid6581 : sid8580;
	}

}
