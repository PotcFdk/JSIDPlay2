package sidplay.audio.xuggle;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.io.XugglerIO;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public abstract class XuggleAudioDriver implements AudioDriver {

	protected OutputStream out;

	private EventScheduler context;

	private IMediaWriter writer;
	private long firstTimeStamp;
	private double ticksPerMicrosecond;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);
		out = getOut(recordingFilename);

		writer = ToolFactory.makeWriter(XugglerIO.map(out));

		IContainerFormat containerFormat = IContainerFormat.make();
		containerFormat.setOutputFormat(getOutputFormatName(), null, null);

		writer.getContainer().setFormat(containerFormat);
		if (writer.addAudioStream(0, 0, getAudioCodec(), cfg.getChannels(), cfg.getFrameRate()) < 0) {
			throw new IOException("Could not add audio stream");
		}
		firstTimeStamp = 0;
		ticksPerMicrosecond = cpuClock.getCpuFrequency() / 1000000;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		long timeStamp = getTimeStamp();

		short[] shortArray = new short[sampleBuffer.position() >> 1];
		((Buffer) sampleBuffer).flip();
		sampleBuffer.asShortBuffer().get(shortArray);

		writer.encodeAudio(0, shortArray, timeStamp, TimeUnit.MICROSECONDS);
	}

	@Override
	public void close() {
		if (writer != null && writer.isOpen()) {
			writer.close();
			writer = null;
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

	private long getTimeStamp() {
		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp == 0) {
			firstTimeStamp = now;
		}
		return (long) ((now - firstTimeStamp) / ticksPerMicrosecond);
	}

	protected abstract String getOutputFormatName();

	protected abstract ID getAudioCodec();

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;

}
