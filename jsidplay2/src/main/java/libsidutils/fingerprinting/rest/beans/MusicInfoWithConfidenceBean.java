package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import libsidutils.fingerprinting.model.FingerprintedSampleData;
import libsidutils.fingerprinting.model.SongMatch;

@XmlRootElement(name = "musicInfoWithConfidence")
@XmlType(propOrder = { "musicInfo", "confidence", "relativeConfidence", "offsetSeconds", "offset" })
public class MusicInfoWithConfidenceBean {

	private MusicInfoBean musicInfo;
	private double relativeConfidence, offsetSeconds;
	private int confidence, offset;

	public MusicInfoBean getMusicInfo() {
		return musicInfo;
	}

	@XmlElement(name = "musicInfo")
	public void setMusicInfo(MusicInfoBean musicInfoBean) {
		this.musicInfo = musicInfoBean;
	}

	public int getConfidence() {
		return confidence;
	}

	@XmlElement(name = "confidence")
	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public double getRelativeConfidence() {
		return relativeConfidence;
	}

	@XmlElement(name = "relativeConfidence")
	public void setRelativeConfidence(double relativeConfidence) {
		this.relativeConfidence = relativeConfidence;
	}

	public int getOffset() {
		return offset;
	}

	@XmlElement(name = "offset")
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public double getOffsetSeconds() {
		return offsetSeconds;
	}

	@XmlElement(name = "offsetSeconds")
	public void setOffsetSeconds(double offsetSeconds) {
		this.offsetSeconds = offsetSeconds;
	}

	@Override
	public String toString() {
		return musicInfo + ", confidence=" + confidence + ", relativeConfidence=" + relativeConfidence + ", offset="
				+ offset + ", offsetSeconds=" + offsetSeconds;
	}

	public void setSongMatch(FingerprintedSampleData fingerprintedSampleData, SongMatch songMatch) {
		confidence = songMatch.getMatch().getCount();
		relativeConfidence = (songMatch.getMatch().getCount()
				/ (double) fingerprintedSampleData.getFingerprint().getLinkList().size()) * 100;
		offset = songMatch.getMatch().getTime();
		offsetSeconds = songMatch.getMatch().getTime() * 0.03225806451612903;
	}
}
