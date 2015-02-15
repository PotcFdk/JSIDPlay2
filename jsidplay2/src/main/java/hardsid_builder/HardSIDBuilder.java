package hardsid_builder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
	private static final String VERSION = "1.0.1";

	private final ArrayList<HardSID> sidobjs = new ArrayList<HardSID>(4);

	private static HsidDLL2 hsid2;
	private int sid6581, sid8580;

	public HardSIDBuilder(IConfig config) {
		sid8580 = config.getEmulation().getHardsid8580() - 1;
		sid6581 = config.getEmulation().getHardsid6581() - 1;
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
		hsid2 = new HsidDLL2();
		// JNI driver wrapper loads the fake HardSID driver
		// (that internally loads the original HardSID driver)
		if (!hsid2.LoadLibrary(driverPath)) {
			throw new RuntimeException(driverPath + " could not be loaded!");
		}
		hsid2.InitHardSID_Mapper();
	}

	@Override
	public SIDEmu lock(EventScheduler evt, IConfig config, SIDEmu device,
			int sidNum, SidTune tune) {
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulation(),
				tune, sidNum);
		if (device == null) {
			device = lock(evt, chipModel);
		} else {
			device.setChipModel(chipModel);
		}
		return device;
	}

	@Override
	public void unlock(final SIDEmu device) {
		for (HardSID hardSid : sidobjs) {
			hardSid.setChipsUsed(sidobjs.size() - 1);
			hardSid.flush();
			hardSid.reset((byte) 0);
		}
		if (sidobjs.remove(device)) {
			((HardSID) device).lock(false);
		}
	}

	@Override
	public int getNumDevices() {
		return hsid2.GetHardSIDCount();
	}

	@Override
	public void start(EventScheduler context) {
	}

	@Override
	public void setVolume(int num, IAudioSection audio) {
	}

	@Override
	public void setBalance(int num, IAudioSection audio) {
	}

	private String extract(IConfig config, final String path,
			final String libName) throws IOException {
		File f = new File(new File(config.getSidplay2().getTmpDir()), libName);
		if (!f.exists()) {
			f.deleteOnExit();
			saveFile(getClass().getResourceAsStream(path + libName), f);
		}
		return f.getAbsolutePath();
	}

	private void saveFile(final InputStream inputStream, final File outputFile)
			throws IOException {
		try (final BufferedOutputStream bout = new BufferedOutputStream(
				new FileOutputStream(outputFile))) {
			// Transfer bytes from in to out
			final byte[] buf = new byte[1024];
			int len;
			while ((len = inputStream.read(buf)) > 0) {
				bout.write(buf, 0, len);
			}
		}
		inputStream.close();
	}

	private SIDEmu lock(final EventScheduler context, ChipModel model) {
		ChipModel alreadyUsedModel = null;
		if (sidobjs.size() > 0) {
			// Stereo? Use a HardSID different to the first SID
			alreadyUsedModel = sidobjs.get(0).getChipModel();
			if (model == alreadyUsedModel) {
				model = (model == ChipModel.MOS6581) ? ChipModel.MOS8580
						: ChipModel.MOS6581;
			}
		}
		HardSID hsid = new HardSID(context, hsid2,
				model == ChipModel.MOS6581 ? sid6581 : sid8580, model);
		if (hsid.lock(true)) {
			sidobjs.add(hsid);
			for (HardSID hardSid : sidobjs) {
				hardSid.setChipsUsed(sidobjs.size());
			}
			return hsid;
		}
		throw new RuntimeException("HardSID ERROR: No available SIDs to lock");
	}

	public static final String credits() {
		return String.format("HardSID V%s Engine:\n", VERSION)
				+ "\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
	}

}
