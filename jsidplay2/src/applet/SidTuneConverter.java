package applet;

import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileFilter;

import libpsid64.Psid64;
import libsidutils.STIL;
import libsidutils.STIL.STILEntry;
import libsidutils.pucrunch.PUCrunch;
import sidplay.ini.intf.IConfig;
import applet.filefilter.TuneFileFilter;

public class SidTuneConverter {
	/**
	 * file filter for tunes
	 */
	private final FileFilter fFileFilter = new TuneFileFilter();

	protected IConfig config;

	public SidTuneConverter(IConfig cfg) {
		config = cfg;
	}

	public void convertFiles(final File[] files, final File target) {
		for (final File file : files) {
			if (file.isDirectory()) {
				convertFiles(file.listFiles(), target);
			} else {
				if (fFileFilter.accept(file)) {
					convertToPSID64(file, target);
				}
			}
		}
	}

	private void convertToPSID64(final File file, final File target) {
		final String filename = file.getAbsolutePath();
		final Psid64 psid64 = new Psid64();
		psid64.setHVSC(config.getSidplay2().getHvsc());
		psid64.setStilEntry(getSTIL(file));
		if (!psid64.load(file)) {
			System.err.println("filename: " + filename);
			System.err.println(psid64.getStatus());
			return;
		}
		if (!psid64.convert()) {
			System.err.println("filename: " + filename);
			System.err.println(psid64.getStatus());
			return;
		}
		File tmpFile = null;
		try {
			tmpFile = new File(System.getProperty("jsidplay2.tmpdir"),
					PathUtils.getBaseNameNoExt(file) + ".prg.tmp");
			if (!psid64.save(tmpFile.getAbsolutePath())) {
				System.err.println("filename: " + filename);
				System.err.println(psid64.getStatus());
				return;
			}
			// crunch result
			PUCrunch crunch = new PUCrunch();
			crunch.run(new String[] {
					tmpFile.getAbsolutePath(),
					new File(target, PathUtils.getBaseNameNoExt(file) + ".prg")
							.getAbsolutePath() });
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (tmpFile != null) {
				tmpFile.delete();
			}
		}
	}

	private STILEntry getSTIL(final File file) {
		final String name = PathUtils.getHVSCName(config, file);
		if (null != name) {
			STIL stil = STIL.getInstance(config.getSidplay2().getHvsc());
			if (stil != null) {
				return stil.getSTIL(name);
			}
		}
		return null;
	}

}
