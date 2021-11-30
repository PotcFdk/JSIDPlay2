package ui.entities.config;

import static java.util.stream.Collectors.toList;
import static ui.entities.collection.HVSCEntry_.author;
import static ui.entities.collection.HVSCEntry_.released;
import static ui.entities.collection.HVSCEntry_.title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.LazyListField;
import ui.common.properties.ObservableLazyListField;
import ui.common.properties.ShadowField;
import ui.entities.collection.HVSCEntry;

@Entity
@Access(AccessType.PROPERTY)
public class FavoritesSection {

	public static final List<HVSCEntry> DEFAULT_FAVORITES = Collections.emptyList();

	public static List<FavoriteColumn> DEFAULT_COLUMNS;

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlTransient
	@JsonIgnore
	public final Integer getId() {
		return id;
	}

	public final void setId(Integer id) {
		this.id = id;
	}

	private ShadowField<StringProperty, String> name = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getName() {
		return name.get();
	}

	public final void setName(String name) {
		this.name.set(name);
	}

	public final StringProperty nameProperty() {
		return name.property();
	}

	private ShadowField<DoubleProperty, Number> width = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public final Double getWidth() {
		return width.get().doubleValue();
	}

	public final void setWidth(Double width) {
		this.width.set(width);
	}

	public final DoubleProperty widthProperty() {
		return width.property();
	}

	private ShadowField<IntegerProperty, Number> selectedRowFrom = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), -1);

	public final Integer getSelectedRowFrom() {
		return selectedRowFrom.get().intValue();
	}

	public final void setSelectedRowFrom(Integer selectedRowFrom) {
		this.selectedRowFrom.set(selectedRowFrom);
	}

	public final IntegerProperty selectedRowFromProperty() {
		return selectedRowFrom.property();
	}

	private ShadowField<IntegerProperty, Number> selectedRowTo = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), -1);

	public final Integer getSelectedRowTo() {
		return selectedRowTo.get().intValue();
	}

	public final void setSelectedRowTo(Integer selectedRowTo) {
		this.selectedRowTo.set(selectedRowTo);
	}

	public final IntegerProperty selectedRowToProperty() {
		return selectedRowTo.property();
	}

	private LazyListField<FavoriteColumn> columns = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public final List<FavoriteColumn> getColumns() {
		// Singular attributes cannot be resolved that early, therefore lazy:
		if (DEFAULT_COLUMNS == null) {
			DEFAULT_COLUMNS = Arrays.asList(new FavoriteColumn(title), new FavoriteColumn(author),
					new FavoriteColumn(released));
		}
		return columns.get(() -> DEFAULT_COLUMNS.stream().map(FavoriteColumn::new).collect(toList()));
	}

	public final void setColumns(List<FavoriteColumn> columns) {
		this.columns.set(columns);
	}

	private ObservableLazyListField<HVSCEntry> favorites = new ObservableLazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	@XmlElement(name = "favorite")
	public final List<HVSCEntry> getFavorites() {
		return favorites.get(() -> new ArrayList<>(DEFAULT_FAVORITES));
	}

	public final void setFavorites(List<HVSCEntry> favorites) {
		this.favorites.set(favorites);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}