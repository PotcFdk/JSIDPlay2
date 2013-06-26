package ui.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import ui.entities.config.Configuration;
import ui.filefilter.TuneFileFilter;

import libpsid64.Psid64;
import libsidutils.PathUtils;
import libsidutils.STIL;
import libsidutils.pucrunch.PUCrunch;

public class SidTuneConverter {
	/**
	 * file filter for tunes
	 */
	private final FileFilter fFileFilter = new TuneFileFilter();

	protected Configuration config;

	public SidTuneConverter(Configuration cfg) {
		config = cfg;
	}

	public void convertFiles(File hvscRoot, final File[] files, final File target) {
		for (final File file : files) {
			if (file.isDirectory()) {
				convertFiles(hvscRoot, file.listFiles(), target);
			} else {
				if (fFileFilter.accept(file)) {
					convertToPSID64(hvscRoot, file, target);
				}
			}
		}
	}

	private void convertToPSID64(File hvscRoot, final File file, final File target) {
		final String filename = file.getAbsolutePath();
		final Psid64 psid64 = new Psid64();
		psid64.setStilEntry(STIL.getSTIL(hvscRoot, file));
		if (!psid64.load(file)) {
			System.err.println("filename: " + filename);
			System.err.println(psid64.getStatus());
			return;
		}
		if (!psid64.convert(hvscRoot)) {
			System.err.println("filename: " + filename);
			System.err.println(psid64.getStatus());
			return;
		}
		File tmpFile = null;
		try {
			tmpFile = new File(config.getSidplay2().getTmpDir(),
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

}
