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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class Assembly64Column {

	private Integer id;

	public Assembly64Column() {
	}

	public Assembly64Column(Assembly64Column assembly64Column) {
		setColumnType(assembly64Column.getColumnType());
		setWidth(assembly64Column.getWidth());
	}

	public Assembly64Column(Assembly64ColumnType assembly64ColumnType) {
		setColumnType(assembly64ColumnType);
		setWidth(assembly64ColumnType.getDefaultWidth());
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

	private ShadowField<ObjectProperty<Assembly64ColumnType>, Assembly64ColumnType> columnType = new ShadowField<>(
			SimpleObjectProperty::new, null);

	@Enumerated(EnumType.STRING)
	public Assembly64ColumnType getColumnType() {
		return columnType.get();
	}

	public void setColumnType(Assembly64ColumnType columnType) {
		this.columnType.set(columnType);
	}

	public ObjectProperty<Assembly64ColumnType> columnTypeProperty() {
		return columnType.property();
	}

	private ShadowField<DoubleProperty, Number> width = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.floatValue()), 0);

	public double getWidth() {
		return width.get().doubleValue();
	}

	public void setWidth(double width) {
		this.width.set(width);
	}

	public DoubleProperty widthProperty() {
		return width.property();
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
