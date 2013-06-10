package ui.entities.collection;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T22:31:27.457+0100")
@StaticMetamodel(StilEntry.class)
public class StilEntry_ {
	public static volatile SingularAttribute<StilEntry, Integer> id;
	public static volatile SingularAttribute<StilEntry, HVSCEntry> hvscEntry;
	public static volatile SingularAttribute<StilEntry, String> stilName;
	public static volatile SingularAttribute<StilEntry, String> stilAuthor;
	public static volatile SingularAttribute<StilEntry, String> stilTitle;
	public static volatile SingularAttribute<StilEntry, String> stilArtist;
	public static volatile SingularAttribute<StilEntry, String> stilComment;
}
