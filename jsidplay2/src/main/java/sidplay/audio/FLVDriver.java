package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_MP3;

import com.xuggle.xuggler.ICodec.ID;

import sidplay.audio.xuggle.XuggleVideoDriver;

/**
 * Allows FLV file write and as an alternative creating a real-time video stream
 * via RTMP protocol e.g. "rtmp://localhost/live/test" in conjunction with nginx
 * server with installed RTMP module.
 * 
 * Follow instructions here to setup a RTMP enabled web-server:
 * https://programmer.ink/think/5e368f92922ac.html
 * 
 * Supported formats:
 * https://stackoverflow.com/questions/9727590/what-codecs-does-xuggler-support
 * 
 * @author ken
 *
 */
public abstract class FLVDriver extends XuggleVideoDriver {

	/**
	 * File based driver to create a FLV file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLVFileDriver extends FLVDriver {

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	/**
	 * Driver to upload real-time video stream via RTMP protocol to a web
	 * server.<BR>
	 *
	 * E.g "rtmp://localhost/live/test" <B>Note:</B> RTMP enabled web-server must be
	 * running (e.g. nginx + rtmp module)
	 *
	 *
	 * <B>Note:</B> RTMP enabled web-server must be started beforehand (e.g. sudo
	 * /usr/local/nginx/sbin/nginx)
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLVStreamDriver extends FLVDriver {

		private String rtmpUrl;

		public FLVStreamDriver(String rtmpUrl) {
			this.rtmpUrl = rtmpUrl;
		}

		@Override
		protected String getRecordingFilename(String recordingFilename) {
			// Note: a local recording file name is overridden by RTMP URL
			return this.rtmpUrl;
		}

	}

	@Override
	protected String getOutputFormatName() {
		return "flv";
	}

	@Override
	protected ID getVideoCodec() {
		return CODEC_ID_H264;
	}

	@Override
	protected ID getAudioCodec() {
		return CODEC_ID_MP3;
	}

	@Override
	public String getExtension() {
		return ".flv";
	}

}
