package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.config.IAudioSection;
import sidplay.audio.siddump.SIDDumpExtension;
import sidplay.audio.siddump.SidDumpOutput;

public abstract class SIDDumpDriver extends SIDDumpExtension implements AudioDriver, SIDListener {

	/**
	 * File based driver to create a SID dump file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDDumpFileDriver extends SIDDumpDriver {
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
	 * Driver to write into an SID dump stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDDumpStreamDriver extends SIDDumpDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the SID dump to
		 */
		public SIDDumpStreamDriver(OutputStream out) {
			this.out = new PrintStream(out);
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * Print stream to write the encoded MP3 to.
	 */
	protected PrintStream out;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);

		init(cpuClock);
		setTimeInSeconds(false);

		out = new PrintStream(getOut(recordingFilename));

		out.println(String.format("Middle C frequency is $%04X", getMiddleCFreq()));
		out.println();
		out.println(
				"| Frame | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | FCut RC Typ V |");
		out.println(
				"+-------+---------------------------+---------------------------+---------------------------+---------------+");

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public boolean isAborted() {
		return false;
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
		return ".txt";
	}

	@Override
	public void add(SidDumpOutput putput) {
		out.print("| ");
		out.print(putput.getTime());
		out.print(" | ");
		out.print(putput.getFreq(0));
		out.print(" ");
		out.print(putput.getNote(0));
		out.print(" ");
		out.print(putput.getWf(0));
		out.print(" ");
		out.print(putput.getAdsr(0));
		out.print(" ");
		out.print(putput.getPul(0));
		out.print(" | ");
		out.print(putput.getFreq(1));
		out.print(" ");
		out.print(putput.getNote(1));
		out.print(" ");
		out.print(putput.getWf(1));
		out.print(" ");
		out.print(putput.getAdsr(1));
		out.print(" ");
		out.print(putput.getPul(1));
		out.print(" | ");
		out.print(putput.getFreq(2));
		out.print(" ");
		out.print(putput.getNote(2));
		out.print(" ");
		out.print(putput.getWf(2));
		out.print(" ");
		out.print(putput.getAdsr(2));
		out.print(" ");
		out.print(putput.getPul(2));
		out.print(" | ");
		out.print(putput.getFcut());
		out.print(" ");
		out.print(putput.getRc());
		out.print(" ");
		out.print(putput.getTyp());
		out.print(" ");
		out.print(putput.getV());
		out.println(" |");

	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
