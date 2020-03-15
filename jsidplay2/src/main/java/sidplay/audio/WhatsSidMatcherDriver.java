package sidplay.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.whatssid.ReadFile;
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
			throw new IniConfigException("Sampling rate does not match " + 8000);
		}
		out = new ByteArrayOutputStream();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		int length = sampleBuffer.position();
		byte[] buffer = new byte[length];
		sampleBuffer.flip();
		sampleBuffer.get(buffer, 0, length);
		out.write(buffer, 0, length);
	}

	@Override
	public void close() {
		if (out != null) {
			byte target[] = out.toByteArray();
			if (target.length > 0) {
				ReadFile readFile = new ReadFile();
				readFile.readFile(target);
				Index index = new Index();
				String database = System.getenv().getOrDefault("YECHENG_DATABASE_NAME", "musiclibary");
				int port = Integer.parseInt(System.getenv().getOrDefault("YECHENG_DATABASE_PORT", "3306"));
				String host = System.getenv().getOrDefault("YECHENG_DATABASE_HOST", "localhost");
				String user = System.getenv().getOrDefault("YECHENG_DATABASE_USER", "newuser");
				String pass = System.getenv().getOrDefault("YECHENG_DATABASE_PASS", "password");
				MysqlDB db = new MysqlDB(host, port, database, user, pass);
				index.loadDB(db);
				SongMatch song_match = index.search(readFile.fingerprint, 15);
				if (song_match!=null && song_match.getIdSong() != -1) {
					DBMatch result = db.getByID(song_match.getIdSong());
					result.setConfidence(song_match.getMatch().getCount());
					result.setRelativeConfidence(
							(song_match.getMatch().getCount() / (double) readFile.fingerprint.getLinkList().size())
									* 100);
					result.setOffset(song_match.getMatch().getTime());
					result.setOffsetSeconds(song_match.getMatch().getTime() * 0.03225806451612903);
					System.out.println(result.toString());
				} else {
					System.out.println("No match!");
				}
				System.out.println("Matching End");
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
