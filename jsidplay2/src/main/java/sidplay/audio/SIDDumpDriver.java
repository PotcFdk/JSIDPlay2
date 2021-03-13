package sidplay.audio;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class SIDDumpDriver extends SIDDumpExtension implements AudioDriver, SIDListener {

	private PrintStream out;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);

		init(cpuClock);
		setTimeInSeconds(false);

		out = new PrintStream(new BufferedOutputStream(new FileOutputStream(recordingFilename)));

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
		out.close();
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

}
