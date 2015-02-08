package ui.entities.collection;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;

@Generated(value="Dali", date="2012-11-20T21:11:08.525+0100")
@StaticMetamodel(HVSCEntry.class)
public class HVSCEntry_ {
	public static volatile SingularAttribute<HVSCEntry, Integer> id;
	public static volatile SingularAttribute<HVSCEntry, String> path;
	public static volatile SingularAttribute<HVSCEntry, String> name;
	public static volatile SingularAttribute<HVSCEntry, String> title;
	public static volatile SingularAttribute<HVSCEntry, String> author;
	public static volatile SingularAttribute<HVSCEntry, String> released;
	public static volatile SingularAttribute<HVSCEntry, String> format;
	public static volatile SingularAttribute<HVSCEntry, String> playerId;
	public static volatile SingularAttribute<HVSCEntry, Integer> noOfSongs;
	public static volatile SingularAttribute<HVSCEntry, Integer> startSong;
	public static volatile SingularAttribute<HVSCEntry, Clock> clockFreq;
	public static volatile SingularAttribute<HVSCEntry, Speed> speed;
	public static volatile SingularAttribute<HVSCEntry, Model> sidModel1;
	public static volatile SingularAttribute<HVSCEntry, Model> sidModel2;
	public static volatile SingularAttribute<HVSCEntry, Model> sidModel3;
	public static volatile SingularAttribute<HVSCEntry, Compatibility> compatibility;
	public static volatile SingularAttribute<HVSCEntry, Long> tuneLength;
	public static volatile SingularAttribute<HVSCEntry, String> audio;
	public static volatile SingularAttribute<HVSCEntry, Integer> sidChipBase1;
	public static volatile SingularAttribute<HVSCEntry, Integer> sidChipBase2;
	public static volatile SingularAttribute<HVSCEntry, Integer> sidChipBase3;
	public static volatile SingularAttribute<HVSCEntry, Integer> driverAddress;
	public static volatile SingularAttribute<HVSCEntry, Integer> loadAddress;
	public static volatile SingularAttribute<HVSCEntry, Integer> loadLength;
	public static volatile SingularAttribute<HVSCEntry, Integer> initAddress;
	public static volatile SingularAttribute<HVSCEntry, Integer> playerAddress;
	public static volatile SingularAttribute<HVSCEntry, Date> fileDate;
	public static volatile SingularAttribute<HVSCEntry, Long> fileSizeKb;
	public static volatile SingularAttribute<HVSCEntry, Long> tuneSizeB;
	public static volatile SingularAttribute<HVSCEntry, Short> relocStartPage;
	public static volatile SingularAttribute<HVSCEntry, Short> relocNoPages;
	public static volatile SingularAttribute<HVSCEntry, String> stilGlbComment;
	public static volatile ListAttribute<HVSCEntry, StilEntry> stil;
}
