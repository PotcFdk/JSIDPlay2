package ui.entities.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import ui.entities.collection.HVSCEntry;
import ui.entities.collection.HVSCEntry_;


@Entity
public class FavoritesSection {

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

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	@OneToMany(cascade = CascadeType.ALL)
	private List<FavoriteColumn> columns;

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

	@XmlElement(name = "favorite")
	@OneToMany(cascade = CascadeType.ALL)
	private List<HVSCEntry> favorites;

	public void setFavorites(List<HVSCEntry> favorites) {
		this.favorites = favorites;
	}

	@XmlTransient
	public List<HVSCEntry> getFavorites() {
		if (favorites == null) {
			favorites = new ArrayList<HVSCEntry>();
		}
		return favorites;
	}

}