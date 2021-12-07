package ui.entities.whatssid;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import libsidutils.fingerprinting.rest.beans.HashBean;
import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
@Table(name = "HashTable", indexes = { @Index(columnList = "idHashTable", unique = true),
		@Index(columnList = "HASH", name = "hash") })
public class HashTable {

	private int idHashTable;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idHashTable", nullable = false)
	public final int getIdHashTable() {
		return idHashTable;
	}

	public final void setIdHashTable(int idHashTable) {
		this.idHashTable = idHashTable;
	}

	private int hash;

	@Column(name = "HASH", nullable = false)
	public final int getHash() {
		return hash;
	}

	public final void setHash(int hash) {
		this.hash = hash;
	}

	private int id;

	@Column(name = "ID", nullable = false)
	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	private int time;

	@Column(name = "Time", nullable = false)
	public final int getTime() {
		return time;
	}

	public final void setTime(int time) {
		this.time = time;
	}

	public final HashBean toBean() {
		HashBean result = new HashBean();
		result.setHash(hash);
		result.setId(id);
		result.setTime(time);
		return result;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
