package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_AAC;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;

import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
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
		protected String getUrl(IAudioSection audioSection, String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	/**
	 * Driver to upload real-time video stream via RTMP protocol to a web
	 * server.<BR>
	 *
	 * E.g "rtmp://localhost/live/test"
	 * 
	 * <B>Note:</B> RTMP enabled web-server must be running (e.g. nginx + rtmp
	 * module)
	 *
	 * <B>Note:</B> RTMP enabled web-server must be started beforehand (e.g. sudo
	 * /usr/local/nginx/sbin/nginx)
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLVStreamDriver extends FLVDriver {

		private final String url;

		public FLVStreamDriver() {
			this(null);
		}

		public FLVStreamDriver(String url) {
			this.url = url;
		}

		/**
		 * @param audioSection      used to get live streaming URL by configuration
		 * @param recordingFilename gets ignored, live streaming video URL is used,
		 *                          instead. Either by configuration or by constructor
		 */
		@Override
		protected String getUrl(IAudioSection audioSection, String recordingFilename) {
			if (url != null) {
				return url;
			} else {
				return audioSection.getVideoStreamingUrl();
			}
		}

	}

	@Override
	protected String getOutputFormatName() {
		return "flv";
	}

	@Override
	protected List<SamplingRate> getSupportedSamplingRates() {
		return Arrays.asList(SamplingRate.LOW);
	}

	@Override
	protected SamplingRate getDefaultSamplingRate() {
		return SamplingRate.LOW;
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
		return ".flv";
	}

}
