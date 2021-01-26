package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlTransient;

import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
public class FavoriteColumn {
	private Integer id;

	public FavoriteColumn() {
	}

	public FavoriteColumn(FavoriteColumn column) {
		columnProperty = column.columnProperty;
		width = column.width;
	}

	public FavoriteColumn(SingularAttribute<?, ?> attribute) {
		columnProperty = attribute.getName();
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

	private String columnProperty;

	public String getColumnProperty() {
		return columnProperty;
	}

	public void setColumnProperty(String columnProperty) {
		this.columnProperty = columnProperty;
	}

	private Double width;

	public Double getWidth() {
		return width;
	}

	public void setWidth(Double width) {
		this.width = width;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
