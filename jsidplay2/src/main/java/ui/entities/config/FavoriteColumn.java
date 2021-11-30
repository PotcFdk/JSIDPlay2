package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	@JsonIgnore
	public final Integer getId() {
		return id;
	}

	public final void setId(Integer id) {
		this.id = id;
	}

	private ShadowField<StringProperty, String> column = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getColumnProperty() {
		return column.get();
	}

	public final void setColumnProperty(String columnProperty) {
		this.column.set(columnProperty);
	}

	public final StringProperty columnProperty() {
		return column.property();
	}

	private ShadowField<DoubleProperty, Number> width = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.floatValue()), 0);

	public final double getWidth() {
		return width.get().doubleValue();
	}

	public final void setWidth(double width) {
		this.width.set(width);
	}

	public final DoubleProperty widthProperty() {
		return width.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
