package ui.entities.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;
import libsidplay.sidtune.SidTuneInfo;

@Entity
public class HVSCEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Integer id;

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

	@Column(length = 2048)
	private String playerId;

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

	@Enumerated(EnumType.STRING)
	private Clock clockFreq;

	public Clock getClockFreq() {
		return clockFreq;
	}

	public void setClockFreq(Clock clockFreq) {
		this.clockFreq = clockFreq;
	}

	@Enumerated(EnumType.STRING)
	private Speed speed;

	public Speed getSpeed() {
		return speed;
	}

	public void setSpeed(Speed speed) {
		this.speed = speed;
	}

	@Enumerated(EnumType.STRING)
	private Model sidModel1;

	public Model getSidModel1() {
		return sidModel1;
	}

	public void setSidModel1(Model sidModel1) {
		this.sidModel1 = sidModel1;
	}

	@Enumerated(EnumType.STRING)
	private Model sidModel2;

	public Model getSidModel2() {
		return sidModel2;
	}

	public void setSidModel2(Model sidModel2) {
		this.sidModel2 = sidModel2;
	}

	@Enumerated(EnumType.STRING)
	private Compatibility compatibility;

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

	@Column(length = 4096)
	private String stilGlbComment;

	public String getStilGlbComment() {
		return stilGlbComment;
	}

	public void setStilGlbComment(String stilComment) {
		this.stilGlbComment = stilComment;
	}

	@OneToMany(mappedBy = "hvscEntry", fetch = FetchType.LAZY)
	private List<StilEntry> stil;

	public List<StilEntry> getStil() {
		if (stil == null) {
			stil = new ArrayList<StilEntry>();
		}
		return stil;
	}

	public static HVSCEntry create(final Player player, final String path,
			final File tuneFile, SidTune tune) {
		HVSCEntry hvscEntry = new HVSCEntry();
		hvscEntry.setPath(path);
		hvscEntry.setName(tuneFile.getName());
		if (tune != null) {
			tune.setSelectedSong(1);
			SidTuneInfo info = tune.getInfo();

			Iterator<String> descriptionIt = info.infoString.iterator();
			if (descriptionIt.hasNext()) {
				String title = descriptionIt.next();
				hvscEntry.setTitle(title);
			}
			if (descriptionIt.hasNext()) {
				String author = descriptionIt.next();
				hvscEntry.setAuthor(author);
			}
			if (descriptionIt.hasNext()) {
				String released = descriptionIt.next();
				hvscEntry.setReleased(released);
			}
			hvscEntry.setFormat(tune.getClass().getSimpleName());
			hvscEntry.setPlayerId(getPlayer(tune));
			hvscEntry.setNoOfSongs(info.songs);
			hvscEntry.setStartSong(info.startSong);
			hvscEntry.setClockFreq(info.clockSpeed);
			hvscEntry.setSpeed(tune.getSongSpeed(1));
			hvscEntry.setSidModel1(info.sid1Model);
			hvscEntry.setSidModel2(info.sid2Model);
			hvscEntry.setCompatibility(info.compatibility);
			hvscEntry.setTuneLength(Long.valueOf(getTuneLength(player, tune)));
			hvscEntry.setAudio(getAudio(info.sidChipBase2));
			hvscEntry.setSidChipBase1(info.sidChipBase1);
			hvscEntry.setSidChipBase2(info.sidChipBase2);
			hvscEntry.setDriverAddress(info.determinedDriverAddr);
			hvscEntry.setLoadAddress(info.loadAddr);
			hvscEntry.setLoadLength(info.c64dataLen);
			hvscEntry.setInitAddress(info.initAddr);
			hvscEntry.setPlayerAddress(info.playAddr);
			hvscEntry.setFileDate(new Date(tuneFile.lastModified()));
			hvscEntry.setFileSizeKb(tuneFile.length() >> 10);
			hvscEntry.setTuneSizeB(tuneFile.length());
			hvscEntry.setRelocStartPage(info.relocStartPage);
			hvscEntry.setRelocNoPages(info.relocPages);
		}
		return hvscEntry;
	}

	private static int getTuneLength(final Player player, SidTune tune) {
		return player.getSidDatabaseInfo(db -> db.getFullSongLength(tune));
	}

	private static String getPlayer(SidTune tune) {
		StringBuilder ids = new StringBuilder();
		for (String s : tune.identify()) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append(s);
		}
		return ids.toString();
	}

	private static String getAudio(int sidChipBase2) {
		return sidChipBase2 != 0 ? "Stereo" : "Mono";
	}

}
