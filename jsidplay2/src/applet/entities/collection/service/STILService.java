package applet.entities.collection.service;

import java.io.File;
import java.util.ArrayList;

import javax.persistence.EntityManager;

import libsidutils.STIL;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import sidplay.ini.intf.IConfig;
import applet.PathUtils;
import applet.entities.collection.HVSCEntry;

public class STILService {

	private EntityManager em;

	public STILService(EntityManager em) {
		this.em = em;
	};

	public void add(final IConfig config, final File tuneFile,
			HVSCEntry hvscEntry) {
		final STILEntry stilEntry = getSTIL(config, tuneFile);
		if (stilEntry != null) {
			// get STIL Global Comment
			hvscEntry.setStilGlbComment(stilEntry.globalComment);
			// add tune infos
			addSTILInfo(hvscEntry, stilEntry.infos, tuneFile);
			// go through subsongs & add them as well
			for (final TuneEntry entry : stilEntry.subtunes) {
				addSTILInfo(hvscEntry, entry.infos, tuneFile);
			}
		}
	}

	private STILEntry getSTIL(final IConfig config, final File tuneFile) {
		final String name = PathUtils.getHVSCName(config, tuneFile);
		if (null != name) {
			STIL stil = STIL.getInstance(config.getSidplay2().getHvsc());
			if (stil != null) {
				return stil.getSTIL(name);
			}
		}
		return null;
	}

	private void addSTILInfo(HVSCEntry hvscEntry, ArrayList<Info> infos,
			File tuneFile) {
		for (Info info : infos) {
			applet.entities.collection.StilEntry stil = new applet.entities.collection.StilEntry();
			stil.setStilName(info.name);
			stil.setStilAuthor(info.author);
			stil.setStilTitle(info.title);
			stil.setStilArtist(info.artist);
			stil.setStilComment(info.comment);
			stil.setHvscEntry(hvscEntry);
			try {
				em.persist(stil);
			} catch (Throwable e) {
				System.err.println("Tune: " + tuneFile.getAbsolutePath());
				System.err.println(e.getMessage());
			}
			hvscEntry.getStil().add(stil);
		}
	}

	public void clear() {
		em.createQuery("DELETE from STIL").executeUpdate();
	}

}
