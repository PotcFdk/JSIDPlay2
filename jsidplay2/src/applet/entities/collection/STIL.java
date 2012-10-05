package applet.entities.collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class STIL {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne
	private HVSCEntry hvscEntry;

	public HVSCEntry getHvscEntry() {
		return hvscEntry;
	}

	public void setHvscEntry(HVSCEntry hvscEntry) {
		this.hvscEntry = hvscEntry;
	}

	private String stilName;
	
	public String getStilName() {
		return stilName;
	}

	public void setStilName(String name) {
		this.stilName = name;
	}

	private String stilAuthor;

	public String getStilAuthor() {
		return stilAuthor;
	}

	public void setStilAuthor(String author) {
		this.stilAuthor = author;
	}

	private String stilTitle;
	
	public String getStilTitle() {
		return stilTitle;
	}

	public void setStilTitle(String title) {
		this.stilTitle = title;
	}

	private String stilArtist;
	
	public String getStilArtist() {
		return stilArtist;
	}

	public void setStilArtist(String artist) {
		this.stilArtist = artist;
	}

	@Column(length=4096)
	private String stilComment;

	public String getStilComment() {
		return stilComment;
	}

	public void setStilComment(String comment) {
		this.stilComment = comment;
	}
	
}
