package sidplay.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.FingerPrintingCreator;
import libsidutils.fingerprinting.IFingerprintInserter;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;
import sidplay.audio.exceptions.SongEndException;

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
public class WhatsSidDriver implements AudioDriver {

	private ByteBuffer sampleBuffer;

	private WAVHeader wavHeader;

	private OutputStream wav;

	private RandomAccessFile file;

	private String recordingFilename;

	private SidTune tune;

	private String collectionName;

	private IFingerprintInserter fingerprintInserter;

	public void setTune(SidTune tune) {
		this.tune = tune;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public void setFingerprintInserter(IFingerprintInserter fingerprintInserter) {
		this.fingerprintInserter = fingerprintInserter;
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException, InterruptedException {
		this.recordingFilename = recordingFilename;

		if (new File(recordingFilename).exists()) {
			throw new SongEndException();
		}
		System.out.println("Create: " + recordingFilename);

		file = new RandomAccessFile(recordingFilename, "rw");

		wavHeader = new WAVHeader(cfg.getChannels(), cfg.getFrameRate());
		wav = new FileOutputStream(file.getFD());
		wav.write(wavHeader.getBytes());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			wav.write(sampleBuffer.array(), 0, sampleBuffer.position());
			wavHeader.advance(sampleBuffer.position());
		} catch (final IOException e) {
			throw new RuntimeException("Error writing WAV audio stream", e);
		}
	}

	@Override
	public void close() {
		if (wav != null && file != null) {
			try {
				file.seek(0);
				wav.write(wavHeader.getBytes());
				wav.close();

				file.close();
			} catch (IOException e) {
				throw new RuntimeException("Error closing WAV audio stream", e);
			} finally {
				wav = null;
				file = null;
			}
		}
		if (recordingFilename != null && new File(recordingFilename).exists() && fingerprintInserter != null) {
			try {
				System.out.printf("Insert Fingerprint for %s (%d)\n", collectionName,
						tune != SidTune.RESET ? tune.getInfo().getCurrentSong() : 1);

				MusicInfoBean musicInfoBean = createMusicInfoBean(tune, recordingFilename, collectionName);
				WavBean wavBean = new WavBean(Files.readAllBytes(Paths.get(recordingFilename)));

				fingerprintInserter.insert(musicInfoBean, wavBean);
			} catch (IOException e) {
				throw new RuntimeException("Error reading WAV audio stream", e);
			}
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".wav";
	}

	private MusicInfoBean createMusicInfoBean(SidTune tune, String fileDir, String infoDir) {
		if (tune != SidTune.RESET) {
			Iterator<String> descriptionIterator = tune.getInfo().getInfoString().iterator();

			return toMusicInfoBean(tune.getInfo().getCurrentSong(), getNextDescription(descriptionIterator),
					getNextDescription(descriptionIterator), getNextDescription(descriptionIterator), fileDir, infoDir);
		} else {
			return toMusicInfoBean(1, new File(PathUtils.getFilenameWithoutSuffix(infoDir)).getName(), "<???>", "<???>",
					fileDir, infoDir);
		}
	}

	private String getNextDescription(Iterator<String> descriptionIterator) {
		return descriptionIterator.hasNext() ? descriptionIterator.next() : "<???>";
	}

	private MusicInfoBean toMusicInfoBean(int songNo, String title, String artist, String album, String fileDir,
			String infoDir) {
		MusicInfoBean musicInfoBean = new MusicInfoBean();
		musicInfoBean.setSongNo(songNo);
		musicInfoBean.setTitle(title);
		musicInfoBean.setArtist(artist);
		musicInfoBean.setAlbum(album);
		musicInfoBean.setFileDir(fileDir);
		musicInfoBean.setInfoDir(infoDir);
		return musicInfoBean;
	}

}
