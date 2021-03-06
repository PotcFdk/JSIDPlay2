/*
 * AAC audio driver
 * 
 * Quelle: https://videoencoding.websmith.de/encoding-theorie/der-audiocodec-aac.html
 * 
 * AAC ist ein von der MPEG Group (Moving Picture Experts Group) entwickeltes, verlustbehaftetes Audio Kompressionsverfahren.
 * Es wurde als Weiterentwicklung von MPEG-2 im MPEG-2-Standard spezifiziert.
 * Bei der Entwicklung wurden die bekannten Schwächen und Probleme des MP3 Kodierungsverfahrens mit einkalkuliert und
 * erheblich verbessert.
 *
 * Nun, die Klangqualität ist hervorragend. Sie hat jedoch Ihren Preis, und
 * nur sehr wenige Vlog und Videoforen Betreiber werden in die Lizenzkosten investieren.
 * Und an diesem Punkt werden Open Source Lösungen wie FAAC (Free Advanced Audio Codec) interessant.
 * Der AAC Audiocodec hat den großen Vorteil, daß er bereits bei Bitraten um die 64 kb (einigermaßen) gute Qualität
 * in Stereo bieten kann.
 *
 * Für den AAC Audiocodec wurden, wie auch für den H.264 Videocodec, verschiedene Profile definiert.
 * In diesem Fall jedoch von der International Organization for Standardization, kurz ISO genannt.
 * Sie erleichtern vor allem den Hardwareherstellern den Umgang mit diesem Kompressionsverfahren.
 * Die gängisten Profile werden wir hier kurz vorstellen.
 * Low Complexity Profil (AAC-LC)
 *
 * Hauptsächlich für mittlere bis hohe Bitraten gedacht. Dieses Profil wird von den meisten AAC-fähigen
 * Hardware Endgeräten unterstützt. Aufgrund seinen Vorteilen hinsichtlich der Encodier- und Decodiereffizienz
 * und der besseren Klangqualität bei höheren Bitraten ist dieses Profil das am meisten verbreitetste Profil bei
 * allen Hardwareherstellern und den Bereitstellern von Musicstores.
 * Main Profil (AAC Main)
 *
 * Dieses Profil ist für niedrigere bis mittlere Bitraten gedacht. Aufgrund seiner, im Vergleich zu AAC-LC,
 * viel höheren Encodier- und Decodierleistung ist dieses Profil verhältnismäßig uneffizient. Die Klangqualität ist
 * bei mittleren Bitraten besser als die des AAC-LC Profils, rechtfertigt aber keinesfalls diese Uneffizienz.
 * High Efficiency Profil (HE-AAC)
 *
 * Dieses Profil ist ebenfalls für niedrigere bis mittlere Bitraten gedacht.
 * Jedoch bietet es durch die technische Implementierung der Spektralband Replikation, kurz SBR genannt,
 * eine wesentlich bessere Qualität bei sehr niedrigen bis mittleren Bitraten als bei den beiden anderen Profilen.
 * Das High Efficiency Profil für den Audiocodec AAC wird hauptsächlich im Streamingbereich eingesetzt.
 * Bei höheren Bitraten jedoch ist die Klangqualität des AAC-LC Profils wiederum besser.
*/
package sidplay.audio;

import static sidplay.audio.Audio.AAC;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import org.sheinbergon.aac.encoder.AACAudioEncoder;
import org.sheinbergon.aac.encoder.AACAudioOutput;
import org.sheinbergon.aac.encoder.AACAudioOutput.Accumulator;
import org.sheinbergon.aac.encoder.util.AACEncodingProfile;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.OS;
import libsidplay.config.IAudioSection;

public abstract class AACDriver implements AudioDriver {

	/**
	 * File based driver to create a AAC file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AACFile extends AACDriver {

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
					((FileOutputStream) out).close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing FLAC audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an AAC output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AACStream extends AACDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded AAC to
		 */
		public AACStream(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * Output stream to write the encoded AAC to.
	 */
	protected OutputStream out;

	private AACAudioEncoder aacEncoder;
	private int factor;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		out = getOut(recordingFilename);
		try {
			aacEncoder = AACAudioEncoder.builder().channels(cfg.getChannels()).sampleRate(cfg.getFrameRate())
					.profile(AACEncodingProfile.AAC_LC).build();

			sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
					.order(ByteOrder.LITTLE_ENDIAN);

			factor = Math.max(1, sampleBuffer.capacity() / aacEncoder.inputBufferSize());

		} catch (UnsatisfiedLinkError e) {
			System.err.println("Error: Java for Windows, Linux or OSX is required to use " + AAC + " video driver!");
			if (OS.get() == OS.LINUX) {
				System.err.println("Try to install it yourself, use the following command and start JSIDPlay2 again:");
				System.err.println("sudo apt-get install libfdk-aac-dev");
			}
			throw e;
		}
	}

	@Override
	public void write() throws InterruptedException {
		try {
			Accumulator aacAccumulator = AACAudioOutput.accumulator();
			int offset = 0;
			for (int i = 0; i < factor; i++) {

				byte[] buffer = new byte[Math.min(sampleBuffer.position() - offset, aacEncoder.inputBufferSize())];
				System.arraycopy(sampleBuffer.array(), offset, buffer, 0, buffer.length);

				aacEncoder.encode(aacAccumulator, buffer);

				offset += aacEncoder.inputBufferSize();
			}
			out.write(aacAccumulator.done().data());
		} catch (ArrayIndexOutOfBoundsException | IOException e) {
			throw new RuntimeException("Error writing AAC audio stream", e);
		}
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
		return ".aac";
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;

}
