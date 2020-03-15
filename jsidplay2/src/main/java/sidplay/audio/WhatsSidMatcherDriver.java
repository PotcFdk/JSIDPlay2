package sidplay.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.whatssid.FingerprintedSampleData;
import sidplay.audio.whatssid.WhatsSidBaseDriver;
import sidplay.audio.whatssid.database.DBMatch;
import sidplay.audio.whatssid.database.Index;
import sidplay.audio.whatssid.database.MysqlDB;
import sidplay.audio.whatssid.model.SongMatch;
import sidplay.ini.IniConfigException;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the matching part. Use WhatsSidDriver to analyze beforehand.
 * 
 * @author ken
 *
 */
public class WhatsSidMatcherDriver extends WhatsSidBaseDriver {

	private ByteBuffer sampleBuffer;

	private ByteArrayOutputStream out;

	private IAudioSection audioSection;

	@Override
	public void configure(SidTune tune, IAudioSection audioSection) {
		this.audioSection = audioSection;
	}

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException, InterruptedException {
		if (audioSection.getSamplingRate() != SamplingRate.VERY_LOW) {
			audioSection.setSamplingRate(SamplingRate.VERY_LOW);
			throw new IniConfigException("Sampling rate does not match 8000");
		}
		out = new ByteArrayOutputStream();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		int length = sampleBuffer.position();
		((Buffer) sampleBuffer).flip();

		byte[] buffer = new byte[length];
		sampleBuffer.get(buffer, 0, length);

		out.write(buffer, 0, length);
	}

	@Override
	public void close() {
		if (out != null) {
			byte target[] = out.toByteArray();
			if (target.length > 0) {

				FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(target, 0, target.length);

				MysqlDB database = new MysqlDB();
				Index index = new Index(database);

				SongMatch songMatch = index.search(fingerprintedSampleData.getFingerprint(), 15);

				if (songMatch != null && songMatch.getIdSong() != -1) {

					DBMatch result = database.getByID(songMatch.getIdSong());
					result.setSongMatch(fingerprintedSampleData, songMatch);

					System.out.println("Match: " + result.toString());
				} else {
					System.out.println("No match!");
				}
			}
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return false;
	}

}
