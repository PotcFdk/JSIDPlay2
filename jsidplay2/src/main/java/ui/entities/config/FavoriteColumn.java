package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlTransient;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class FavoriteColumn {
	private Integer id;

	public FavoriteColumn() {
	}

	public FavoriteColumn(FavoriteColumn column) {
		setColumnProperty(column.getColumnProperty());
		setWidth(column.getWidth());
	}

	public FavoriteColumn(SingularAttribute<?, ?> attribute) {
		setColumnProperty(attribute.getName());
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

	private ShadowField<StringProperty, String> column = new ShadowField<>(SimpleStringProperty::new, null);

	public String getColumnProperty() {
		return column.get();
	}

	public void setColumnProperty(String columnProperty) {
		this.column.set(columnProperty);
	}

	public StringProperty columnProperty() {
		return column.property();
	}

	private ShadowField<DoubleProperty, Number> width = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.floatValue()), 0);

	public double getWidth() {
		return width.get().doubleValue();
	}

	public void setWidth(double width) {
		this.width.set(width);
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
