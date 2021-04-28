package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

public abstract class SIDRegDriver implements SIDListener, AudioDriver {

	/**
	 * File based driver to create a SID reg file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDRegFileDriver extends SIDRegDriver {
		@Override
		protected OutputStream getOut(String recordingFilename) throws IOException {
			System.out.println("Recording, file=" + recordingFilename);
			return new FileOutputStream(recordingFilename);
		}

		@Override
		public void close() {
			super.close();
			if (out != null) {
				try {
					out.close();
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an SID reg stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDRegStreamDriver extends SIDRegDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the SID reg to
		 */
		public SIDRegStreamDriver(OutputStream out) {
			this.out = new PrintStream(out);
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.audio.SIDRegDriver");

	/**
	 * Print stream to write the encoded MP3 to.
	 */
	protected PrintStream out;

	private EventScheduler context;

	private long fTime;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);
		this.context = context;

		out = new PrintStream(getOut(recordingFilename));

		fTime = 0;
		writeHeader(out);

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

		new SidRegWrite(time, relTime, addr, data).writeSidRegister(out);

		fTime = time;
	}

	@Override
	public void write() throws InterruptedException {
	}

	@Override
	public void close() {
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

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
