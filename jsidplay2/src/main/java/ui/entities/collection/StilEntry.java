package ui.entities.collection;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import libsidutils.stil.STIL.Info;
import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
public class StilEntry {

	public StilEntry() {
	}

	public StilEntry(StilEntry stilEntry) {
		stilName = stilEntry.stilName;
		stilAuthor = stilEntry.stilAuthor;
		stilTitle = stilEntry.stilTitle;
		stilArtist = stilEntry.stilArtist;
		stilComment = stilEntry.stilComment;
	}

	public StilEntry(Info info) {
		stilName = info.name;
		stilAuthor = info.author;
		stilTitle = info.title;
		stilArtist = info.artist;
		stilComment = info.comment;
	}

	private int id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private HVSCEntry hvscEntry;

	@ManyToOne
	public final HVSCEntry getHvscEntry() {
		return hvscEntry;
	}

	public final void setHvscEntry(HVSCEntry hvscEntry) {
		this.hvscEntry = hvscEntry;
	}

	private String stilName;

	public final String getStilName() {
		return stilName;
	}

	public final void setStilName(String name) {
		this.stilName = name;
	}

	private String stilAuthor;

	public final String getStilAuthor() {
		return stilAuthor;
	}

	public final void setStilAuthor(String author) {
		this.stilAuthor = author;
	}

	private String stilTitle;

	public final String getStilTitle() {
		return stilTitle;
	}

	public final void setStilTitle(String title) {
		this.stilTitle = title;
	}

	private String stilArtist;

	public final String getStilArtist() {
		return stilArtist;
	}

	public final void setStilArtist(String artist) {
		this.stilArtist = artist;
	}

	private String stilComment;

	@Column(length = 4096)
	public final String getStilComment() {
		return stilComment;
	}

	public final void setStilComment(String comment) {
		this.stilComment = comment;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
