package ui.entities.whatssid;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(MusicInfo.class)
public abstract class MusicInfo_ {

	public static volatile SingularAttribute<MusicInfo, Long> idMusicInfo;
	public static volatile SingularAttribute<MusicInfo, String> artist;
	public static volatile SingularAttribute<MusicInfo, String> album;
	public static volatile SingularAttribute<MusicInfo, String> fileDir;
	public static volatile SingularAttribute<MusicInfo, Double> audioLength;
	public static volatile SingularAttribute<MusicInfo, String> infoDir;
	public static volatile SingularAttribute<MusicInfo, String> title;

}

