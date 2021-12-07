package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
@Table(name = "GAMES", indexes = { @Index(name = "GAMES_NAME", columnList = "NAME", unique = false),
		@Index(name = "GAMES_MU_ID", columnList = "MU_ID", unique = false),
		@Index(name = "GAMES_GE_ID", columnList = "GE_ID", unique = false),
		@Index(name = "GAMES_PU_ID", columnList = "PU_ID", unique = false),
		@Index(name = "GAMES_PR_ID", columnList = "PR_ID", unique = false),
		@Index(name = "GAMES_COMMENT_INDEX", columnList = "COMMENT", unique = false) })
public class Games {
	private int id;

	@Id
	@Column(name = "GA_ID")
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private String name;

	@Column(name = "NAME")
	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	private Years years;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "YE_ID")
	public final Years getYears() {
		return years;
	}

	public final void setYears(Years years) {
		this.years = years;
	}

	private String filename;

	@Column(name = "FILENAME")
	public final String getFilename() {
		return filename;
	}

	public final void setFilename(String filename) {
		this.filename = filename;
	}

	private String fileToRun;

	@Column(name = "FILETORUN")
	public final String getFileToRun() {
		return fileToRun;
	}

	public final void setFileToRun(String fileToRun) {
		this.fileToRun = fileToRun;
	}

	private int filenameIdx;

	@Column(name = "FILENAMEINDEX")
	public final int getFilenameIdx() {
		return filenameIdx;
	}

	public final void setFilenameIdx(int filenameIdx) {
		this.filenameIdx = filenameIdx;
	}

	private String screenshotFilename;

	@Column(name = "SCRNSHOTFILENAME")
	public final String getScreenshotFilename() {
		return screenshotFilename;
	}

	public final void setScreenshotFilename(String screenshotFilename) {
		this.screenshotFilename = screenshotFilename;
	}

	private Musicians musicians;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MU_ID")
	public final Musicians getMusicians() {
		return musicians;
	}

	public final void setMusicians(Musicians musicians) {
		this.musicians = musicians;
	}

	private Genres genres;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "GE_ID")
	public final Genres getGenres() {
		return genres;
	}

	public final void setGenres(Genres genres) {
		this.genres = genres;
	}

	private Publishers publishers;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PU_ID")
	public final Publishers getPublishers() {
		return publishers;
	}

	public final void setPublishers(Publishers publishers) {
		this.publishers = publishers;
	}

	private String sidFilename;

	@Column(name = "SIDFILENAME")
	public final String getSidFilename() {
		return sidFilename;
	}

	public final void setSidFilename(String sidFilename) {
		this.sidFilename = sidFilename;
	}

	private Programmers programmers;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PR_ID")
	public final Programmers getProgrammers() {
		return programmers;
	}

	public final void setProgrammers(Programmers programmers) {
		this.programmers = programmers;
	}

	private String comment;

	@Column(name = "COMMENT")
	public final String getComment() {
		return comment;
	}

	public final void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
