package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_AAC;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;

import com.xuggle.xuggler.ICodec.ID;

import sidplay.audio.xuggle.XuggleVideoDriver;

public abstract class MP4Driver extends XuggleVideoDriver {

	/**
	 * File based driver to create a MP4 file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class MP4FileDriver extends MP4Driver {

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	@Override
	protected String getOutputFormatName() {
		return "mpeg4";
	}

	@Override
	protected ID getVideoCodec() {
		return CODEC_ID_H264;
	}

	@Override
	protected ID getAudioCodec() {
		return CODEC_ID_AAC;
	}

	@Override
	public String getExtension() {
		return ".mp4";
	}

}
