package ui.entities.whatssid;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
@Table(name = "MusicInfo", indexes = { @Index(columnList = "idMusicInfo", name = "idMusicInfo_UNIQUE", unique = true),
		@Index(columnList = "songNo", name = "songNo", unique = false) })
public class MusicInfo {

	private int idMusicInfo;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idMusicInfo", nullable = false)
	public final int getIdMusicInfo() {
		return idMusicInfo;
	}

	public final void setIdMusicInfo(int idMusicInfo) {
		this.idMusicInfo = idMusicInfo;
	}

	private Integer songNo;

	@Column(name = "songNo")
	public final Integer getSongNo() {
		return songNo;
	}

	public final void setSongNo(Integer songNo) {
		this.songNo = songNo;
	}

	private String title;

	@Column(name = "Title", nullable = false)
	public final String getTitle() {
		return title;
	}

	public final void setTitle(String title) {
		this.title = title;
	}

	private String artist;

	@Column(name = "Artist", nullable = false)
	public final String getArtist() {
		return artist;
	}

	public final void setArtist(String artist) {
		this.artist = artist;
	}

	private String album;

	@Column(name = "Album", nullable = false)
	public final String getAlbum() {
		return album;
	}

	public final void setAlbum(String album) {
		this.album = album;
	}

	private String fileDir;

	@Column(name = "FileDir")
	public final String getFileDir() {
		return fileDir;
	}

	public final void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	private String infoDir;

	@Column(name = "InfoDir")
	public final String getInfoDir() {
		return infoDir;
	}

	public final void setInfoDir(String infoDir) {
		this.infoDir = infoDir;
	}

	private double audioLength;

	@Column(name = "audio_length")
	public final double getAudioLength() {
		return audioLength;
	}

	public final void setAudioLength(double audioLength) {
		this.audioLength = audioLength;
	}

	@Transient
	public final MusicInfoBean toBean() {
		MusicInfoBean result = new MusicInfoBean();
		result.setSongNo(songNo);
		result.setTitle(title);
		result.setArtist(artist);
		result.setAlbum(album);
		result.setFileDir(fileDir);
		result.setInfoDir(infoDir);
		result.setAudioLength(audioLength);
		return result;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
