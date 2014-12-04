package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.sidtune.SidTune;
import lowlevel.LameEncoder;

/**
 * File based driver to create a MP3 file.
 * 
 * @author Ken Händel
 * 
 */
public class MP3File extends AudioDriver {

	private static final String EXTENSION = ".mp3";
	/**
	 * Sample buffer to be encoded as MP3.
	 */
	private ByteBuffer sampleBuffer;
	/**
	 * Output stream to write the encoded MP3 to.
	 */
	private OutputStream out;
	/**
	 * Jump3r encoder.
	 */
	private LameEncoder jump3r;

	@Override
	public void open(final AudioConfig cfg, SidTune tune)
			throws LineUnavailableException, UnsupportedAudioFileException,
			IOException {
		final int channels = cfg.channels;
		final int blockAlign = 2 * channels;

		// We need to make a buffer for the user
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * blockAlign);
		sampleBuffer.order(ByteOrder.LITTLE_ENDIAN);
		AudioFormat audioFormat = new AudioFormat(cfg.frameRate, 16,
				cfg.channels, true, false);
		jump3r = new LameEncoder(audioFormat);
		out = new FileOutputStream(recordingFilenameProvider.getFilename(tune)
				+ EXTENSION);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			byte[] encoded = new byte[jump3r.getMP3BufferSize()];
			int bytesWritten = jump3r.encodeBuffer(sampleBuffer.array(), 0,
					sampleBuffer.capacity(), encoded);
			out.write(encoded, 0, bytesWritten);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			throw new InterruptedException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InterruptedException();
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void close() {
		if (jump3r != null) {
			jump3r.close();
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
