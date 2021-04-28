package sidplay.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import net.sourceforge.javaflacencoder.EncodingConfiguration;
import net.sourceforge.javaflacencoder.FLACEncoder;
import net.sourceforge.javaflacencoder.FLACFileOutputStream;
import net.sourceforge.javaflacencoder.FLACOutputStream;
import net.sourceforge.javaflacencoder.FLACStreamOutputStream;
import net.sourceforge.javaflacencoder.StreamConfiguration;

public abstract class FLACDriver implements AudioDriver {

	/**
	 * File based driver to create a FLAC file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLACFileDriver extends FLACDriver {

		@Override
		protected FLACOutputStream getOut(String recordingFilename) throws IOException {
			System.out.println("Recording, file=" + recordingFilename);
			return new FLACFileOutputStream(recordingFilename);
		}

		@Override
		public void close() {
			super.close();
			if (out != null) {
				try {
					((FLACFileOutputStream) out).close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing FLAC audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an FLAC output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLACStreamDriver extends FLACDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded FLAC to
		 */
		public FLACStreamDriver(OutputStream out) {
			try {
				this.out = new FLACStreamOutputStream(out);
			} catch (IOException e) {
				throw new RuntimeException("Error creating FLAC stream audio stream", e);
			}
		}

		@Override
		protected FLACOutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * Output stream to write the encoded FLAC to.
	 */
	protected FLACOutputStream out;

	private FLACEncoder flacEncoder;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);

		out = getOut(recordingFilename);

		StreamConfiguration sc = new StreamConfiguration();
		sc.setBitsPerSample(Short.SIZE);
		sc.setChannelCount(cfg.getChannels());
		sc.setSampleRate(cfg.getFrameRate());

		EncodingConfiguration ec = new EncodingConfiguration();

		flacEncoder = new FLACEncoder();
		flacEncoder.setStreamConfiguration(sc);
		flacEncoder.setOutputStream(out);
		flacEncoder.openFLACStream();
		flacEncoder.setEncodingConfiguration(ec);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		int len = sampleBuffer.position();
		((Buffer) sampleBuffer).flip();

		int[] samples = new int[len >> 1];
		for (int i = 0; i < len >> 1; i++) {
			samples[i] = sampleBuffer.getShort();
		}
		int count = len >> 2;
		flacEncoder.addSamples(samples, count);
		try {
			flacEncoder.encodeSamples(count, false);
		} catch (IOException e) {
			throw new RuntimeException("Error writing FLAC audio stream", e);
		}
	}

	@Override
	public void close() {
		try {
			if (flacEncoder != null) {
				flacEncoder.encodeSamples(0, true);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error writing FLAC audio stream", e);
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
		return ".flac";
	}

	protected abstract FLACOutputStream getOut(String recordingFilename) throws IOException;
}
