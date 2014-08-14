package ui.entities.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

@Entity
public class ViewEntity {
	
	public ViewEntity() {
	}
	
	public ViewEntity(String fxId) {
		this.fxId = fxId;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

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
