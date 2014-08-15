package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@Access(AccessType.PROPERTY)
public class ViewEntity {
	
	public ViewEntity() {
	}
	
	public ViewEntity(String fxId) {
		this.fxId = fxId;
	}
	
	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String fxId;
	
	public String getFxId() {
		return fxId;
	}
	
	public void setFxId(String fxId) {
		this.fxId = fxId;
	}
}
