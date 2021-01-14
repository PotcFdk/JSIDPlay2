package ui.entities.config;

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
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
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
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private ShadowField<StringProperty, String> name = new ShadowField<>(SimpleStringProperty::new, null);

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return name.property();
	}

	private ShadowField<DoubleProperty, Number> width = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public Double getWidth() {
		return width.get().doubleValue();
	}

	public void setWidth(Double width) {
		this.width.set(width);
	}

	public DoubleProperty widthProperty() {
		return width.property();
	}

	private ShadowField<IntegerProperty, Number> selectedRowFrom = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), -1);

	public Integer getSelectedRowFrom() {
		return selectedRowFrom.get().intValue();
	}

	public void setSelectedRowFrom(Integer selectedRowFrom) {
		this.selectedRowFrom.set(selectedRowFrom);
	}

	public IntegerProperty selectedRowFromProperty() {
		return selectedRowFrom.property();
	}

	private ShadowField<IntegerProperty, Number> selectedRowTo = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), -1);

	public Integer getSelectedRowTo() {
		return selectedRowTo.get().intValue();
	}

	public void setSelectedRowTo(Integer selectedRowTo) {
		this.selectedRowTo.set(selectedRowTo);
	}

	public IntegerProperty selectedRowToProperty() {
		return selectedRowTo.property();
	}

	private LazyListField<FavoriteColumn> columns = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<FavoriteColumn> getColumns() {
		// Singular attributes cannot be resolved that early, therefore lazy:
		if (DEFAULT_COLUMNS == null) {
			DEFAULT_COLUMNS = Arrays.asList(new FavoriteColumn(title), new FavoriteColumn(author),
					new FavoriteColumn(released));
		}
		return columns.get(new ArrayList<>(DEFAULT_COLUMNS));
	}

	public void setColumns(List<FavoriteColumn> columns) {
		this.columns.set(columns);
	}

	private ObservableLazyListField<HVSCEntry> favorites = new ObservableLazyListField<>();

	public void setFavorites(List<HVSCEntry> favorites) {
		this.favorites.set(favorites);
	}

	@OneToMany(cascade = CascadeType.ALL)
	@XmlElement(name = "favorite")
	public List<HVSCEntry> getFavorites() {
		return favorites.get(new ArrayList<>(DEFAULT_FAVORITES));
	}

	@Transient
	public ObservableList<HVSCEntry> getObservableFavorites() {
		return favorites.getObservableList();
	}

}