package libsidutils.fingerprinting.model;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;

public class MusicInfoWithConfidence {

	private MusicInfoBean musicInfoBean;
	private double relativeConfidence, offsetSeconds;
	private int confidence, offset;

	public MusicInfoBean getMusicInfoBean() {
		return musicInfoBean;
	}

	public void setMusicInfoBean(MusicInfoBean musicInfoBean) {
		this.musicInfoBean = musicInfoBean;
	}

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public double getRelativeConfidence() {
		return relativeConfidence;
	}

	public void setRelativeConfidence(double relativeConfidence) {
		this.relativeConfidence = relativeConfidence;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public double getOffsetSeconds() {
		return offsetSeconds;
	}

	public void setOffsetSeconds(double offsetSeconds) {
		this.offsetSeconds = offsetSeconds;
	}

	@Override
	public String toString() {
		return musicInfoBean + ", confidence=" + confidence + ", relativeConfidence=" + relativeConfidence + ", offset="
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
