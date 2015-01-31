package ui.entities.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToIntFunction;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlTransient;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;
import libsidplay.sidtune.SidTuneInfo;

@Entity
@Access(AccessType.PROPERTY)
public class HVSCEntry {
	public HVSCEntry() {
	}

	public HVSCEntry(final ToIntFunction<SidTune> lengthFnct,
			final String path, final File tuneFile, SidTune tune) {
		this.name = tuneFile.getName();
		this.path = path.length() > 0 ? path : tuneFile.getPath();
		if (tune != null) {
			tune.setSelectedSong(1);
			SidTuneInfo info = tune.getInfo();
			Iterator<String> descriptionIt = info.getInfoString().iterator();
			if (descriptionIt.hasNext()) {
				this.title = descriptionIt.next();
			}
			if (descriptionIt.hasNext()) {
				this.author = descriptionIt.next();
			}
			if (descriptionIt.hasNext()) {
				this.released = descriptionIt.next();
			}
			this.format = tune.getClass().getSimpleName();
			StringBuilder ids = new StringBuilder();
			tune.identify().stream().forEach(id -> ids.append(',').append(id));
			this.playerId = ids.length() > 0 ? ids.substring(1) : ids
					.toString();
			this.noOfSongs = info.getSongs();
			this.startSong = info.getStartSong();
			this.clockFreq = info.getClockSpeed();
			this.speed = tune.getSongSpeed(1);
			this.sidModel1 = info.getSid1Model();
			this.sidModel2 = info.getSid2Model();
			this.compatibility = info.getCompatibility();
			this.tuneLength = Long.valueOf(lengthFnct != null ? lengthFnct
					.applyAsInt(tune) : 0);
			this.audio = info.getSidChipBase(1) != 0
					? info.getSidChipBase(2) != 0 ? "3-SID" : "Stereo"
					: "Mono";
			this.sidChipBase1 = info.getSidChipBase(0);
			this.sidChipBase2 = info.getSidChipBase(1);
			this.driverAddress = info.getDeterminedDriverAddr();
			this.loadAddress = info.getLoadAddr();
			this.loadLength = info.getC64dataLen();
			this.initAddress = info.getInitAddr();
			this.playerAddress = info.getPlayAddr();
			this.fileDate = new Date(tuneFile.lastModified());
			this.fileSizeKb = tuneFile.length() >> 10;
			this.tuneSizeB = tuneFile.length();
			this.relocStartPage = info.getRelocStartPage();
			this.relocNoPages = info.getRelocPages();
		}
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private String author;

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	private String released;

	public String getReleased() {
		return released;
	}

	public void setReleased(String released) {
		this.released = released;
	}

	private String format;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	private String playerId;

	@Column(length = 2048)
	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	private Integer noOfSongs;

	public Integer getNoOfSongs() {
		return noOfSongs;
	}

	public void setNoOfSongs(Integer noOfSongs) {
		this.noOfSongs = noOfSongs;
	}

	private Integer startSong;

	public Integer getStartSong() {
		return startSong;
	}

	public void setStartSong(Integer startSong) {
		this.startSong = startSong;
	}

	private Clock clockFreq;

	@Enumerated(EnumType.STRING)
	public Clock getClockFreq() {
		return clockFreq;
	}

	public void setClockFreq(Clock clockFreq) {
		this.clockFreq = clockFreq;
	}

	private Speed speed;

	@Enumerated(EnumType.STRING)
	public Speed getSpeed() {
		return speed;
	}

	public void setSpeed(Speed speed) {
		this.speed = speed;
	}

	private Model sidModel1;

	@Enumerated(EnumType.STRING)
	public Model getSidModel1() {
		return sidModel1;
	}

	public void setSidModel1(Model sidModel1) {
		this.sidModel1 = sidModel1;
	}

	private Model sidModel2;

	@Enumerated(EnumType.STRING)
	public Model getSidModel2() {
		return sidModel2;
	}

	public void setSidModel2(Model sidModel2) {
		this.sidModel2 = sidModel2;
	}

	private Compatibility compatibility;

	@Enumerated(EnumType.STRING)
	public Compatibility getCompatibility() {
		return compatibility;
	}

	public void setCompatibility(Compatibility compatibility) {
		this.compatibility = compatibility;
	}

	private Long tuneLength;

	public Long getTuneLength() {
		return tuneLength;
	}

	public void setTuneLength(Long tuneLength) {
		this.tuneLength = tuneLength;
	}

	private String audio;

	public String getAudio() {
		return audio;
	}

	public void setAudio(String audio) {
		this.audio = audio;
	}

	private Integer sidChipBase1;

	public Integer getSidChipBase1() {
		return sidChipBase1;
	}

	public void setSidChipBase1(Integer sidChipBase1) {
		this.sidChipBase1 = sidChipBase1;
	}

	private Integer sidChipBase2;

	public Integer getSidChipBase2() {
		return sidChipBase2;
	}

	public void setSidChipBase2(Integer sidChipBase2) {
		this.sidChipBase2 = sidChipBase2;
	}

	private Integer driverAddress;

	public Integer getDriverAddress() {
		return driverAddress;
	}

	public void setDriverAddress(Integer driverAddress) {
		this.driverAddress = driverAddress;
	}

	private Integer loadAddress;

	public Integer getLoadAddress() {
		return loadAddress;
	}

	public void setLoadAddress(Integer loadAddress) {
		this.loadAddress = loadAddress;
	}

	private Integer loadLength;

	public Integer getLoadLength() {
		return loadLength;
	}

	public void setLoadLength(Integer loadLength) {
		this.loadLength = loadLength;
	}

	private Integer initAddress;

	public Integer getInitAddress() {
		return initAddress;
	}

	public void setInitAddress(Integer initAddress) {
		this.initAddress = initAddress;
	}

	private Integer playerAddress;

	public Integer getPlayerAddress() {
		return playerAddress;
	}

	public void setPlayerAddress(Integer playerAddress) {
		this.playerAddress = playerAddress;
	}

	private Date fileDate;

	public Date getFileDate() {
		return fileDate;
	}

	public void setFileDate(Date fileDate) {
		this.fileDate = fileDate;
	}

	private Long fileSizeKb;

	public Long getFileSizeKb() {
		return fileSizeKb;
	}

	public void setFileSizeKb(Long fileSizeInKb) {
		this.fileSizeKb = fileSizeInKb;
	}

	private Long tuneSizeB;

	public Long getTuneSizeB() {
		return tuneSizeB;
	}

	public void setTuneSizeB(Long tuneSizeInB) {
		this.tuneSizeB = tuneSizeInB;
	}

	private Short relocStartPage;

	public Short getRelocStartPage() {
		return relocStartPage;
	}

	public void setRelocStartPage(Short relocStartPage) {
		this.relocStartPage = relocStartPage;
	}

	private Short relocNoPages;

	public Short getRelocNoPages() {
		return relocNoPages;
	}

	public void setRelocNoPages(Short relocNoPages) {
		this.relocNoPages = relocNoPages;
	}

	private String stilGlbComment;

	@Column(length = 4096)
	public String getStilGlbComment() {
		return stilGlbComment;
	}

	public void setStilGlbComment(String stilComment) {
		this.stilGlbComment = stilComment;
	}

	private List<StilEntry> stil;

	@OneToMany(mappedBy = "hvscEntry", fetch = FetchType.LAZY)
	public List<StilEntry> getStil() {
		if (stil == null) {
			stil = new ArrayList<StilEntry>();
		}
		return stil;
	}

	public void setStil(List<StilEntry> stil) {
		this.stil = stil;
	}

}
