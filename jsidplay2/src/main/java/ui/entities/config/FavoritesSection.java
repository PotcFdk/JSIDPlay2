package ui.entities.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;

@Entity
public class FavoritesSection {

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

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private Double width;
	
	public Double getWidth() {
		return width;
	}
	
	public void setWidth(Double width) {
		this.width = width;
	}

	private Integer selectedRowFrom = -1;

	public Integer getSelectedRowFrom() {
		return selectedRowFrom;
	}

	public void setSelectedRowFrom(Integer selectedRowFrom) {
		this.selectedRowFrom = selectedRowFrom;
	}

	private Integer selectedRowTo = -1;

	public Integer getSelectedRowTo() {
		return selectedRowTo;
	}

	public void setSelectedRowTo(Integer selectedRowTo) {
		this.selectedRowTo = selectedRowTo;
	}

	private List<FavoriteColumn> columns;

	@OneToMany(cascade = CascadeType.ALL)
	public List<FavoriteColumn> getColumns() {
		if (columns == null) {
			columns = new ArrayList<FavoriteColumn>();
			FavoriteColumn dbFavoriteColumn;
			SingularAttribute<?, ?> field;
			dbFavoriteColumn = new FavoriteColumn();
			field = (SingularAttribute<?, ?>) HVSCEntry_.title;
			dbFavoriteColumn.setColumnProperty(((Field) field.getJavaMember())
					.getName());
			columns.add(dbFavoriteColumn);
			dbFavoriteColumn = new FavoriteColumn();
			field = (SingularAttribute<?, ?>) HVSCEntry_.author;
			dbFavoriteColumn.setColumnProperty(((Field) field.getJavaMember())
					.getName());
			columns.add(dbFavoriteColumn);
			dbFavoriteColumn = new FavoriteColumn();
			field = (SingularAttribute<?, ?>) HVSCEntry_.released;
			dbFavoriteColumn.setColumnProperty(((Field) field.getJavaMember())
					.getName());
			columns.add(dbFavoriteColumn);
		}
		return columns;
	}

	public void setColumns(List<FavoriteColumn> columns) {
		this.columns = columns;
	}

	protected List<HVSCEntry> favorites;

	protected ObservableList<HVSCEntry> observableFavorites;

	public void setFavorites(List<HVSCEntry> favorites) {
		this.favorites = favorites;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@XmlElement(name = "favorite")
	public List<HVSCEntry> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<HVSCEntry>();
		}
		return getObservableFavorites();
	}

	@Transient
	public ObservableList<HVSCEntry> getObservableFavorites() {
		if (observableFavorites == null) {
			observableFavorites = FXCollections
					.<HVSCEntry> observableArrayList(favorites);
			Bindings.bindContent(favorites, observableFavorites);
		}
		return observableFavorites;
	}

}