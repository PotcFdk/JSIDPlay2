package sidplay.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.WAVDriver.WavHeader;
import sidplay.audio.whatssid.ReadFile;
import sidplay.audio.whatssid.WhatsSidBaseDriver;
import sidplay.audio.whatssid.database.MysqlDB;
import sidplay.ini.IniConfigException;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the analyzing part. Use WhatsSidMatcherDriver to match.
 * 
 * @author ken
 *
 */
public class WhatsSidDriver extends WhatsSidBaseDriver {

	private ByteBuffer sampleBuffer;

	private SidTune tune;

	private IAudioSection audioSection;

	protected WavHeader wavHeader;

	private FileOutputStream wav;

	private RandomAccessFile file;

	private String recordingFilename;

	@Override
	public void configure(SidTune tune, IAudioSection audioSection) {
		this.audioSection = audioSection;
		this.tune = tune;
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException, InterruptedException {
		if (audioSection.getSamplingRate() != SamplingRate.VERY_LOW) {
			audioSection.setSamplingRate(SamplingRate.VERY_LOW);
			throw new IniConfigException("Sampling rate does not match " + 8000);
		}
		this.recordingFilename = recordingFilename;

		System.out.println("Analyzing: " + recordingFilename);
		if (new File(recordingFilename).exists()) {
			throw new NextTuneException();
		}
		wavHeader = new WavHeader(cfg.getChannels(), cfg.getFrameRate());

		file = new RandomAccessFile(recordingFilename, "rw");
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
				int hLength = WAVDriver.WavHeader.HEADER_LENGTH;
				byte[] bytes = Files.readAllBytes(Paths.get(recordingFilename));
				byte[] target = new byte[bytes.length - hLength];
				System.arraycopy(bytes, hLength, target, 0, target.length);
				if (target.length > 0) {
					ReadFile readFile = new ReadFile();
					readFile.readFile(target);
					readFile.getMetaInfo(tune, recordingFilename);

					String database = System.getenv().getOrDefault("YECHENG_DATABASE_NAME", "musiclibary");
					int port = Integer.parseInt(System.getenv().getOrDefault("YECHENG_DATABASE_PORT", "3306"));
					String host = System.getenv().getOrDefault("YECHENG_DATABASE_HOST", "localhost");
					String user = System.getenv().getOrDefault("YECHENG_DATABASE_USER", "newuser");
					String pass = System.getenv().getOrDefault("YECHENG_DATABASE_PASS", "password");
					MysqlDB db = new MysqlDB(host, port, database, user, pass);
					System.out.println("Insert into Database");
					db.insert(readFile, recordingFilename);
					System.out.println("Analyzing End");
				}
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
