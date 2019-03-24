package ui.entities.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

@Embeddable
@Access(AccessType.PROPERTY)
public class Assembly64Section {

	private final List<Assembly64Column> INITIAL_COLUMNS;
	{
		INITIAL_COLUMNS = new ArrayList<>();
		Assembly64Column categoryColumn = new Assembly64Column();
		categoryColumn.setColumnType(Assembly64ColumnType.CATEGORY);
		categoryColumn.setWidth(Assembly64ColumnType.CATEGORY.getDefaultWidth());
		INITIAL_COLUMNS.add(categoryColumn);
		Assembly64Column nameColumn = new Assembly64Column();
		nameColumn.setColumnType(Assembly64ColumnType.NAME);
		nameColumn.setWidth(Assembly64ColumnType.NAME.getDefaultWidth());
		INITIAL_COLUMNS.add(nameColumn);
		Assembly64Column groupColumn = new Assembly64Column();
		groupColumn.setColumnType(Assembly64ColumnType.GROUP);
		groupColumn.setWidth(Assembly64ColumnType.GROUP.getDefaultWidth());
		INITIAL_COLUMNS.add(groupColumn);
		Assembly64Column eventColumn = new Assembly64Column();
		eventColumn.setColumnType(Assembly64ColumnType.EVENT);
		eventColumn.setWidth(Assembly64ColumnType.EVENT.getDefaultWidth());
		INITIAL_COLUMNS.add(eventColumn);
		Assembly64Column ratingColumn = new Assembly64Column();
		ratingColumn.setColumnType(Assembly64ColumnType.RATING);
		ratingColumn.setWidth(Assembly64ColumnType.RATING.getDefaultWidth());
		INITIAL_COLUMNS.add(ratingColumn);
		Assembly64Column yearColumn = new Assembly64Column();
		yearColumn.setColumnType(Assembly64ColumnType.YEAR);
		yearColumn.setWidth(Assembly64ColumnType.YEAR.getDefaultWidth());
		INITIAL_COLUMNS.add(yearColumn);
	}

	private List<Assembly64Column> columns = INITIAL_COLUMNS;

	@OneToMany(cascade = CascadeType.ALL)
	public List<Assembly64Column> getColumns() {
		if (columns == null) {
			columns = new ArrayList<Assembly64Column>();
		}
		return columns;
	}

	public void setColumns(List<Assembly64Column> columns) {
		this.columns = columns;
	}

}
