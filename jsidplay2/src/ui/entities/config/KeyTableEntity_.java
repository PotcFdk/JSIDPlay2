package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.components.keyboard.KeyTableEntry;

@Generated(value="Dali", date="2013-05-13T22:16:50.878+0200")
@StaticMetamodel(KeyTableEntity.class)
public class KeyTableEntity_ {
	public static volatile SingularAttribute<KeyTableEntity, Integer> id;
	public static volatile SingularAttribute<KeyTableEntity, String> keyCodeName;
	public static volatile SingularAttribute<KeyTableEntity, KeyTableEntry> entry;
}
