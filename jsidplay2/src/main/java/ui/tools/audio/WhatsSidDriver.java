package ui.tools.audio;

import static libsidutils.PathUtils.getFilenameWithoutSuffix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.IFingerprintInserter;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;
import sidplay.audio.WAVDriver.WavFileDriver;
import sidplay.audio.exceptions.SongEndException;
import ui.tools.FingerPrintingCreator;

/**
 * WhatsSID? is a Shazam like feature. It analyzes tunes to recognize a
 * currently played tune
 *
 * This is the audio driver to create a fingerprint for a tune. Use
 * {@link FingerPrintingCreator} to create the whole database. <BR>
 * <B>Note:</B> WAV file is created if not exists containing 8KHz sample data.
 * WAV file contents is then fingerprint'ed
 *
 * @author ken
 *
 */
public class WhatsSidDriver extends WavFileDriver {

	private static final String TAG_UNKNOWN = "<???>";

	private String recordingFilename, collectionName;

	private SidTune tune;

	private IFingerprintInserter fingerprintInserter;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.recordingFilename = recordingFilename;

		if (new File(recordingFilename).exists()) {
			throw new SongEndException();
		}
		super.open(audioSection, recordingFilename, cpuClock, context);
	}

	@Override
	public void close() {
		super.close();
		try {
			if (new File(recordingFilename).exists()) {
				int songNo = tune != SidTune.RESET ? tune.getInfo().getCurrentSong() : 1;
				System.out.printf("Insert Fingerprint for %s (%d)\n", collectionName, songNo);

				WavBean wavBean = new WavBean(Files.readAllBytes(Paths.get(recordingFilename)));

				fingerprintInserter.insert(createMusicInfoBean(songNo), wavBean);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading WAV audio stream", e);
		}
	}

	public void setTune(SidTune tune) {
		this.tune = tune;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public void setFingerprintInserter(IFingerprintInserter fingerprintInserter) {
		this.fingerprintInserter = fingerprintInserter;
	}

	private MusicInfoBean createMusicInfoBean(int songNo) {
		String title, author, released;

		if (tune != SidTune.RESET) {
			Iterator<String> descriptionIt = tune.getInfo().getInfoString().iterator();
			title = descriptionIt.hasNext() ? descriptionIt.next() : TAG_UNKNOWN;
			author = descriptionIt.hasNext() ? descriptionIt.next() : TAG_UNKNOWN;
			released = descriptionIt.hasNext() ? descriptionIt.next() : TAG_UNKNOWN;
		} else {
			title = new File(getFilenameWithoutSuffix(collectionName)).getName();
			author = TAG_UNKNOWN;
			released = TAG_UNKNOWN;
		}
		return toMusicInfoBean(songNo, title, author, released);
	}

	private MusicInfoBean toMusicInfoBean(int songNo, String title, String author, String released) {
		MusicInfoBean musicInfoBean = new MusicInfoBean();
		musicInfoBean.setSongNo(songNo);
		musicInfoBean.setTitle(title);
		musicInfoBean.setArtist(author);
		musicInfoBean.setAlbum(released);
		musicInfoBean.setFileDir(recordingFilename);
		musicInfoBean.setInfoDir(collectionName);
		return musicInfoBean;
	}

}
