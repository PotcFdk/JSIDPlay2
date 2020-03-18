package libsidutils.fingerprinting.database;

import libsidutils.fingerprinting.data.FingerprintedSampleData;
import libsidutils.fingerprinting.model.SongMatch;

public class DBMatch {

	private String title, artist, album;
	private double audioLength, relativeConfidence, offsetSeconds;
	private int confidence, offset;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public double getAudioLength() {
		return audioLength;
	}

	public void setAudioLength(double audioLength) {
		this.audioLength = audioLength;
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
		return "title=" + title + ", artist=" + artist + ", album=" + album + ", confidence=" + confidence
				+ ", relativeConfidence=" + relativeConfidence + ", offset=" + offset + ", offsetSeconds="
				+ offsetSeconds;
	}

	public void setSongMatch(FingerprintedSampleData fingerprintedSampleData, SongMatch songMatch) {
		confidence = songMatch.getMatch().getCount();
		relativeConfidence = (songMatch.getMatch().getCount()
				/ (double) fingerprintedSampleData.getFingerprint().getLinkList().size()) * 100;
		offset = songMatch.getMatch().getTime();
		offsetSeconds = songMatch.getMatch().getTime() * 0.03225806451612903;
	}
}
