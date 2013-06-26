package ui.entities.gamebase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "GAMES")
public class Games {
	@Id
	@Column(name = "GA_ID")
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "NAME")
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "YE_ID")
	private Years years;

	public Years getYears() {
		return years;
	}

	public void setYears(Years years) {
		this.years = years;
	}

	@Column(name = "FILENAME")
	private String filename;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Column(name = "FILETORUN")
	private String fileToRun;

	public String getFileToRun() {
		return fileToRun;
	}

	public void setFileToRun(String fileToRun) {
		this.fileToRun = fileToRun;
	}

	@Column(name = "FILENAMEINDEX")
	private int filenameIdx;

	public int getFilenameIdx() {
		return filenameIdx;
	}

	public void setFilenameIdx(int filenameIdx) {
		this.filenameIdx = filenameIdx;
	}

	@Column(name = "SCRNSHOTFILENAME")
	private String screenshotFilename;

	public String getScreenshotFilename() {
		return screenshotFilename;
	}

	public void setScreenshotFilename(String screenshotFilename) {
		this.screenshotFilename = screenshotFilename;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MU_ID")
	private Musicians musicians;

	public Musicians getMusicians() {
		return musicians;
	}

	public void setMusician(Musicians musicians) {
		this.musicians = musicians;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "GE_ID")
	private Genres genres;

	public Genres getGenres() {
		return genres;
	}

	public void setGenres(Genres genres) {
		this.genres = genres;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PU_ID")
	private Publishers publishers;

	public Publishers getPublishers() {
		return publishers;
	}

	public void setPublishers(Publishers publishers) {
		this.publishers = publishers;
	}

	@Column(name = "SIDFILENAME")
	private String sidFilename;

	public String getSidFilename() {
		return sidFilename;
	}

	public void setSidFilename(String sidFilename) {
		this.sidFilename = sidFilename;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PR_ID")
	private Programmers programmers;

	public Programmers getProgrammers() {
		return programmers;
	}

	public void setProgrammers(Programmers programmers) {
		this.programmers = programmers;
	}

	@Column(name = "COMMENT")
	private String comment;

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
