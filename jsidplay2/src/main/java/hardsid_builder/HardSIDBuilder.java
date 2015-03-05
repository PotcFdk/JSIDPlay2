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
import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IAudioSection;
import sidplay.ini.intf.IConfig;

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
 * * This program is free software; you can redistribute it and/or modify * it
 * under the terms of the GNU General Public License as published by * the Free
 * Software Foundation; either version 2 of the License, or * (at your option)
 * any later version. * *
 * **************************************************************************
 * </pre>
 * 
 * @author Ken Händel
 * 
 */
public class HardSIDBuilder implements SIDBuilder {

	/**
	 * Output buffer size.
	 */
	public static final int MAX_BUFFER_SIZE = 1 << 20;

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
	public HardSIDBuilder(final IConfig config) {
		// Extract fake HardSID driver recognizing fake and real devices
		String driverPath;
		try {
			driverPath = extract(config, "/hardsid/cpp/Debug/", "HardSID.dll");
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("HARDSID ERROR: HardSID.dll not found"));
		}
		// Extract original HardSID4U driver, loaded by the driver above
		try {
			extract(config, "/hardsid/cpp/Debug/", "HardSID_orig.dll");
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("HARDSID ERROR: HardSID_orig.dll not found"));
		}
		// Extract JNI driver wrapper
		try {
			System.load(extract(config, "/hardsid_builder/cpp/Debug/",
					"JHardSID.dll"));
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("HARDSID ERROR: JHardSID.dll not found!"));
		}
		// Go and use JNI driver wrapper
		hsidDLL = new HsidDLL2();
		// JNI driver wrapper loads the fake HardSID driver
		// (that internally loads the original HardSID driver)
		if (!hsidDLL.LoadLibrary(driverPath)) {
			throw new RuntimeException(driverPath + " could not be loaded!");
		}
		hsidDLL.InitHardSID_Mapper();
	}

	@Override
	public SIDEmu lock(EventScheduler context, IConfig config,
			SIDEmu oldHardSID, int sidNum, SidTune tune) {
		// we cannot reconfigure already existing HardSIDs
		if (oldHardSID == null) {
			final ChipModel chipModel = getChipModel(config, tune, sidNum);
			final int deviceIdx = getModelDependantDevice(config, chipModel);
			HardSID hsid = new HardSID(this, context, hsidDLL, deviceIdx,
					chipModel);
			if (hsid.lock(true)) {
				sids.add(hsid);
				return hsid;
			}
		}
		return oldHardSID;
	}

	@Override
	public void unlock(final SIDEmu device) {
		HardSID hardSid = (HardSID) device;
		if (sids.remove(device) && hardSid.lock(false)) {
			hardSid.flush();
			hardSid.reset((byte) 0);
		}
	}

	@Override
	public int getNumDevices() {
		return hsidDLL.GetHardSIDCount();
	}

	public final int getDevcesInUse() {
		return sids.size();
	}

	@Override
	public void reset() {
	}

	@Override
	public void start() {
	}

	@Override
	public void setVolume(int num, IAudioSection audio) {
	}

	@Override
	public void setBalance(int num, IAudioSection audio) {
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
	private String extract(final IConfig config, final String path,
			final String libName) throws IOException {
		File f = new File(new File(config.getSidplay2().getTmpDir()), libName);
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
	private void writeResource(final String resource, final File file)
			throws IOException {
		try (InputStream is = getClass().getResourceAsStream(resource);
				OutputStream os = new BufferedOutputStream(
						new FileOutputStream(file))) {
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
	 * @param config
	 *            configuration
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * @return desired chip model
	 */
	private ChipModel getChipModel(IConfig config, SidTune tune, int sidNum) {
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulation(),
				tune, sidNum);
		if (sids.size() > 0) {
			// Stereo device? Use a HardSID device different to the first SID
			ChipModel modelAlreadyInUse = sids.get(0).getChipModel();
			if (chipModel == modelAlreadyInUse) {
				chipModel = (chipModel == ChipModel.MOS6581) ? ChipModel.MOS8580
						: ChipModel.MOS6581;
			}
		}
		return chipModel;
	}

	/**
	 * Get device index based on the desired chip model.
	 * 
	 * @param config
	 *            configuration
	 * @param chipModel
	 *            desired chip model
	 * @return device index of the desired HardSID device
	 */
	private int getModelDependantDevice(final IConfig config,
			final ChipModel chipModel) {
		int sid6581 = config.getEmulation().getHardsid6581();
		int sid8580 = config.getEmulation().getHardsid8580();
		return chipModel == ChipModel.MOS6581 ? sid6581 : sid8580;
	}

}
