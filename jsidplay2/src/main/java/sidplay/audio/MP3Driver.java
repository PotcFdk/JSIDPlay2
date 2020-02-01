package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.config.IAudioSection;
import lowlevel.LameEncoder;
import mp3.MPEGMode;
import sidplay.audio.CmpMP3File.MP3Termination;

/**
 * Abstract base class to output an MP3 encoded tune to an output stream.
 * 
 * @author Ken Händel
 * 
 */
public abstract class MP3Driver implements AudioDriver {

	/**
	 * File based driver to create a MP3 file.
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class MP3File extends MP3Driver {
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
				} catch (IOException e) {
					throw new RuntimeException("Error closing MP3 audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an MP3 encoded output stream.<BR>
	 * 
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 * 
	 * @author Ken Händel
	 * 
	 */
	public static class MP3Stream extends MP3Driver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 * 
		 * @param out Output stream to write the encoded MP3 to
		 */
		public MP3Stream(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	private  int cbr = LameEncoder.DEFAULT_BITRATE;
	private  int vbrQuality = LameEncoder.DEFAULT_QUALITY;
	private boolean vbr = LameEncoder.DEFAULT_VBR;
	
	/**
	 * Jump3r encoder.
	 */
	private LameEncoder jump3r;
	/**
	 * Output stream to write the encoded MP3 to.
	 */
	protected OutputStream out;
	/**
	 * Sample buffer to be encoded as MP3.
	 */
	protected ByteBuffer sampleBuffer;

	@Override
	public void configure(IAudioSection audioSection) {
		cbr = audioSection.getCbr();
		vbr = audioSection.isVbr();
		vbrQuality = audioSection.getVbrQuality();
	}
	
	@Override
	public void open(final AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat audioFormat = new AudioFormat(cfg.getFrameRate(), Short.SIZE, cfg.getChannels(), signed, bigEndian);
		jump3r = new LameEncoder(audioFormat, cbr, MPEGMode.STEREO, vbrQuality, vbr);
		out = getOut(recordingFilename);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			byte[] encoded = new byte[jump3r.getMP3BufferSize()];
			int bytesWritten = jump3r.encodeBuffer(sampleBuffer.array(), 0, sampleBuffer.position(), encoded);
			out.write(encoded, 0, bytesWritten);
		} catch (ArrayIndexOutOfBoundsException | IOException e) {
			throw new MP3Termination(e);
		}
	}

	@Override
	public void close() {
		if (jump3r != null) {
			jump3r.close();
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
		return ".mp3";
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
