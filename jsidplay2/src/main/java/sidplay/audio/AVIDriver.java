package sidplay.audio;

import com.xuggle.xuggler.ICodec.ID;

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
	protected ID getVideoCodec() {
		return ID.CODEC_ID_H264;
	}

	@Override
	protected ID getAudioCodec() {
		return ID.CODEC_ID_PCM_S16LE;
	}

	@Override
	public String getExtension() {
		return ".avi";
	}

}
