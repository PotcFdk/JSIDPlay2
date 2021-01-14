package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@Access(AccessType.PROPERTY)
public class Assembly64Column {

	private Integer id;

	public Assembly64Column() {
	}

	public Assembly64Column(Assembly64ColumnType category) {
		columnType = category;
		width = category.getDefaultWidth();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@XmlTransient
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Enumerated(EnumType.STRING)
	private Assembly64ColumnType columnType;

	public Assembly64ColumnType getColumnType() {
		return columnType;
	}

	public void setColumnType(Assembly64ColumnType columnType) {
		this.columnType = columnType;
	}

	private Double width;

	public Double getWidth() {
		return width;
	}

	public void setWidth(Double width) {
		this.width = width;
	}
}
