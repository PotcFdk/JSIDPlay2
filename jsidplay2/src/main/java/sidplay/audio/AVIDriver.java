package sidplay.audio;

import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import sidplay.audio.xuggle.XuggleVideoDriver;

/**
 * File based driver to create a AVI file.
 *
 * @author Ken Händel
 *
 */
public abstract class AVIDriver extends XuggleVideoDriver {

	/**
	 * File based driver to create a AVI file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AVIFileDriver extends AVIDriver {

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	@Override
	protected String getOutputFormatName() {
		return "avi";
	}

	@Override
	protected List<SamplingRate> getSupportedSamplingRates() {
		return Arrays.asList(SamplingRate.VERY_LOW, SamplingRate.LOW, SamplingRate.MEDIUM);
	}

	@Override
	protected SamplingRate getDefaultSamplingRate() {
		return SamplingRate.LOW;
	}

	@Override
	protected ID getVideoCodec() {
		return ID.CODEC_ID_H264;
	}

	@Override
	protected ID getAudioCodec() {
		return ID.CODEC_ID_MP3;
	}

	@Override
	public String getExtension() {
		return ".avi";
	}

}
