package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.components.keyboard.KeyTableEntry;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(KeyTableEntity.class)
public abstract class KeyTableEntity_ {

	public static volatile SingularAttribute<KeyTableEntity, KeyTableEntry> entry;
	public static volatile SingularAttribute<KeyTableEntity, String> keyCodeName;
	public static volatile SingularAttribute<KeyTableEntity, Integer> id;

}

