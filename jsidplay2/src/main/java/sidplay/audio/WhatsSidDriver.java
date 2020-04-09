package sidplay.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.FingerPrintingCreator;
import sidplay.audio.WAVDriver.WavHeader;
import sidplay.audio.exceptions.NextTuneException;
import sidplay.fingerprinting.FingerprintInserter;

/**
 * WhatsSid? is a Shazam like feature. It analyzes tunes to recognize a currently
 * played tune
 * 
 * This is the audio driver to create a fingerprint for a tune. Use
 * {@link FingerPrintingCreator} to create the whole database.
 * <BR>
 * <B>Note:</B> WAV file is created if not exists containing 8KHz sample data. WAV file
 * contents is then fingerprint'ed
 * 
 * @author ken
 *
 */
public class WhatsSidDriver implements AudioDriver {

	private ByteBuffer sampleBuffer;

	private WavHeader wavHeader;

	private OutputStream wav;

	private RandomAccessFile file;

	private String recordingFilename;

	private File tuneFile;

	private SidTune tune;

	private IConfig config;

	private FingerprintInserter fingerprintInserter;
	
	public void setTuneFile(File file) {
		this.tuneFile = file;
	}

	public void setFingerprintInserter(FingerprintInserter fingerprintInserter) {
		this.fingerprintInserter = fingerprintInserter;
	}
	
	@Override
	public void configure(SidTune tune, IConfig config) {
		this.tune = tune;
		this.config = config;
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException, InterruptedException {
		this.recordingFilename = recordingFilename;

		if (new File(recordingFilename).exists()) {
			throw new NextTuneException();
		}
		System.out.println("Create: " + recordingFilename);

		file = new RandomAccessFile(recordingFilename, "rw");

		wavHeader = new WavHeader(cfg.getChannels(), cfg.getFrameRate());
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
		if (recordingFilename != null && new File(recordingFilename).exists()) {
			try {
				System.out.println("Insert Fingerprint for " + tuneFile.getAbsolutePath());

				File theCollectionFile = new File(config.getSidplay2Section().getHvsc());
				String collectionName = PathUtils.getCollectionName(theCollectionFile, tuneFile);

				fingerprintInserter.insert(tune, collectionName, recordingFilename);
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

}
