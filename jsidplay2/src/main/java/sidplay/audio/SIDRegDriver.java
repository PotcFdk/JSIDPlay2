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
import sidplay.audio.sidreg.SidRegWrite;

public class SIDRegDriver implements SIDListener, AudioDriver {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.audio.SIDRegDriver");

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
