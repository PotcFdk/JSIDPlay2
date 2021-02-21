package sidplay.audio;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ResourceBundle;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.config.IAudioSection;

public class SidRegDriver implements SIDListener, AudioDriver {

	public static class SidRegWrite {
		private long absCycles, relCycles;
		private int address, value;

		public SidRegWrite(long absCycles, long relCycles, int address, byte value) {
			this.absCycles = absCycles;
			this.relCycles = relCycles;
			this.address = address;
			this.value = value & 0xff;
		}

		public Long getAbsCycles() {
			return absCycles;
		}

		public long getRelCycles() {
			return relCycles;
		}

		public String getAddress() {
			return String.format("$%04X", address);
		}

		public String getValue() {
			return String.format("$%02X", value);
		}

		public String getDescription() {
			return BUNDLE.getString(DESCRIPTION[address & 0x1f]);
		}

		public void writeSidRegister(PrintStream printStream) {
			printStream.printf("\"%d\", \"%d\", \"$%04X\", \"$%02X\", \"%s\"\n", absCycles, relCycles, address, value,
					getDescription());
		}

	}

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.audio.SidRegDriver");

	public static final String DESCRIPTION[] = new String[] { "VOICE_1_FREQ_L", "VOICE_1_FREQ_H", "VOICE_1_PULSE_L",
			"VOICE_1_PULSE_H", "VOICE_1_CTRL", "VOICE_1_AD", "VOICE_1_SR", "VOICE_2_FREQ_L", "VOICE_2_FREQ_H",
			"VOICE_2_PULSE_L", "VOICE_2_PULSE_H", "VOICE_2_CTRL", "VOICE_2_AD", "VOICE_2_SR", "VOICE_3_FREQ_L",
			"VOICE_3_FREQ_H", "VOICE_3_PULSE_L", "VOICE_3_PULSE_H", "VOICE_3_CTRL", "VOICE_3_AD", "VOICE_3_SR",
			"FCUT_L", "FCUT_H", "FRES", "FVOL", "PADDLE1", "PADDLE2", "OSC3", "ENV3", "UNUSED", "UNUSED", "UNUSED" };

	private EventScheduler context;

	private PrintStream printStream;
	private long fTime;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);
		this.context = context;
		printStream = new PrintStream(new File(recordingFilename));

		fTime = 0;
		writeHeader(printStream);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write(final int addr, final byte data) {
		final long time = context.getTime(Event.Phase.PHI2);
		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;

		new SidRegWrite(time, relTime, addr, data).writeSidRegister(printStream);

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

	public static void writeHeader(PrintStream printStream) {
		printStream.printf("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"\n", BUNDLE.getString("ABSOLUTE_CYCLES"),
				BUNDLE.getString("RELATIVE_CYCLES"), BUNDLE.getString("ADDRESS"), BUNDLE.getString("VALUE"),
				BUNDLE.getString("DESCRIPTION"));
	}

}
