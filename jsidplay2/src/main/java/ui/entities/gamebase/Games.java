package ui.entities.gamebase;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Access(AccessType.PROPERTY)
@Table(name = "GAMES")
public class Games {
	private int id;

	@Id
	@Column(name = "GA_ID")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String name;

	@Column(name = "NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private Years years;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "YE_ID")
	public Years getYears() {
		return years;
	}

	public void setYears(Years years) {
		this.years = years;
	}

	private String filename;

	@Column(name = "FILENAME")
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	private String fileToRun;

	@Column(name = "FILETORUN")
	public String getFileToRun() {
		return fileToRun;
	}

	public void setFileToRun(String fileToRun) {
		this.fileToRun = fileToRun;
	}

	private int filenameIdx;

	@Column(name = "FILENAMEINDEX")
	public int getFilenameIdx() {
		return filenameIdx;
	}

	public void setFilenameIdx(int filenameIdx) {
		this.filenameIdx = filenameIdx;
	}

	private String screenshotFilename;

	@Column(name = "SCRNSHOTFILENAME")
	public String getScreenshotFilename() {
		return screenshotFilename;
	}

	public void setScreenshotFilename(String screenshotFilename) {
		this.screenshotFilename = screenshotFilename;
	}

	private Musicians musicians;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MU_ID")
	public Musicians getMusicians() {
		return musicians;
	}

	public void setMusicians(Musicians musicians) {
		this.musicians = musicians;
	}

	private Genres genres;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "GE_ID")
	public Genres getGenres() {
		return genres;
	}

	public void setGenres(Genres genres) {
		this.genres = genres;
	}

	private Publishers publishers;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PU_ID")
	public Publishers getPublishers() {
		return publishers;
	}

	public void setPublishers(Publishers publishers) {
		this.publishers = publishers;
	}

	private String sidFilename;

	@Column(name = "SIDFILENAME")
	public String getSidFilename() {
		return sidFilename;
	}

	public void setSidFilename(String sidFilename) {
		this.sidFilename = sidFilename;
	}

	private Programmers programmers;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PR_ID")
	public Programmers getProgrammers() {
		return programmers;
	}

	public void setProgrammers(Programmers programmers) {
		this.programmers = programmers;
	}

	private String comment;

	@Column(name = "COMMENT")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
