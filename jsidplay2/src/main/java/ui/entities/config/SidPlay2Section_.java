package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import ui.favorites.PlaybackType;
import ui.favorites.RepeatType;

@Generated(value="Dali", date="2013-06-09T13:36:37.558+0200")
@StaticMetamodel(SidPlay2Section.class)
public class SidPlay2Section_ {
	public static volatile SingularAttribute<SidPlay2Section, Integer> version;
	public static volatile SingularAttribute<SidPlay2Section, Boolean> enableDatabase;
	public static volatile SingularAttribute<SidPlay2Section, Integer> playLength;
	public static volatile SingularAttribute<SidPlay2Section, Integer> recordLength;
	public static volatile SingularAttribute<SidPlay2Section, PlaybackType> playbackType;
	public static volatile SingularAttribute<SidPlay2Section, RepeatType> repeatType;
	public static volatile SingularAttribute<SidPlay2Section, String> HVMEC;
	public static volatile SingularAttribute<SidPlay2Section, String> demos;
	public static volatile SingularAttribute<SidPlay2Section, String> mags;
	public static volatile SingularAttribute<SidPlay2Section, String> cgsc;
	public static volatile SingularAttribute<SidPlay2Section, String> hvsc;
	public static volatile SingularAttribute<SidPlay2Section, Boolean> single;
	public static volatile SingularAttribute<SidPlay2Section, Boolean> enableProxy;
	public static volatile SingularAttribute<SidPlay2Section, String> proxyHostname;
	public static volatile SingularAttribute<SidPlay2Section, Integer> proxyPort;
	public static volatile SingularAttribute<SidPlay2Section, String> lastDirectory;
	public static volatile SingularAttribute<SidPlay2Section, String> tmpDir;
	public static volatile SingularAttribute<SidPlay2Section, Integer> frameX;
	public static volatile SingularAttribute<SidPlay2Section, Integer> frameY;
	public static volatile SingularAttribute<SidPlay2Section, Integer> frameWidth;
	public static volatile SingularAttribute<SidPlay2Section, Integer> frameHeight;
	public static volatile SingularAttribute<SidPlay2Section, Double> videoScaling;
}
