package sidplay.audio;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ResourceBundle;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.SIDListener;
import ui.sidreg.SidRegWrite;

public class SidRegDriver implements SIDListener, AudioDriver {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.audio.SidRegDriver");

	public static final String DESCRIPTION[] = new String[] { "VOICE_1_FREQ_L", "VOICE_1_FREQ_H", "VOICE_1_PULSE_L",
			"VOICE_1_PULSE_H", "VOICE_1_CTRL", "VOICE_1_AD", "VOICE_1_SR", "VOICE_2_FREQ_L", "VOICE_2_FREQ_H",
			"VOICE_2_PULSE_L", "VOICE_2_PULSE_H", "VOICE_2_CTRL", "VOICE_2_AD", "VOICE_2_SR", "VOICE_3_FREQ_L",
			"VOICE_3_FREQ_H", "VOICE_3_PULSE_L", "VOICE_3_PULSE_H", "VOICE_3_CTRL", "VOICE_3_AD", "VOICE_3_SR",
			"FCUT_L", "FCUT_H", "FRES", "FVOL", "PADDLE1", "PADDLE2", "OSC3", "ENV3", };

	private PrintStream printStream;
	private long fTime;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		System.out.println("Recording, file=" + recordingFilename);
		printStream = new PrintStream(new File(recordingFilename));

		fTime = 0;

		writeHeader(printStream);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write(long time, int addr, byte data) {
		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;

		final SidRegWrite sidRegWrite = new SidRegWrite(time, relTime, addr, BUNDLE.getString(DESCRIPTION[addr & 0x1f]),
				data & 0xff);

		writeSidRegister(printStream, sidRegWrite);

		fTime = time;
	}

	@Override
	public void write() throws InterruptedException {
	}

	@Override
	public void close() {
		printStream.close();
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
		return ".csv";
	}

	public static void writeHeader(PrintStream ps) {
		ps.printf("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"\n", BUNDLE.getString("ABSOLUTE_CYCLES"),
				BUNDLE.getString("RELATIVE_CYCLES"), BUNDLE.getString("ADDRESS"), BUNDLE.getString("VALUE"),
				BUNDLE.getString("DESCRIPTION"));
	}

	public static void writeSidRegister(PrintStream ps, final SidRegWrite sidRegWrite) {
		ps.printf("\"%d\", \"%d\", \"$%04X\", \"$%02X\", \"%s\"\n", sidRegWrite.getAbsCycles(),
				sidRegWrite.getRelCycles(), sidRegWrite.getAddress(), sidRegWrite.getValue(),
				sidRegWrite.getDescription());
	}

}
