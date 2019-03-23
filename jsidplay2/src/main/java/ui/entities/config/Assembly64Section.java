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

	private final List<Column> INITIAL_COLUMNS;
	{
		INITIAL_COLUMNS = new ArrayList<>();
		Column categoryColumn = new Column();
		categoryColumn.setColumnType(ColumnType.CATEGORY);
		categoryColumn.setWidth(ColumnType.CATEGORY.getDefaultWidth());
		INITIAL_COLUMNS.add(categoryColumn);
		Column nameColumn = new Column();
		nameColumn.setColumnType(ColumnType.NAME);
		nameColumn.setWidth(ColumnType.NAME.getDefaultWidth());
		INITIAL_COLUMNS.add(nameColumn);
		Column groupColumn = new Column();
		groupColumn.setColumnType(ColumnType.GROUP);
		groupColumn.setWidth(ColumnType.GROUP.getDefaultWidth());
		INITIAL_COLUMNS.add(groupColumn);
		Column eventColumn = new Column();
		eventColumn.setColumnType(ColumnType.EVENT);
		eventColumn.setWidth(ColumnType.EVENT.getDefaultWidth());
		INITIAL_COLUMNS.add(eventColumn);
		Column ratingColumn = new Column();
		ratingColumn.setColumnType(ColumnType.RATING);
		ratingColumn.setWidth(ColumnType.RATING.getDefaultWidth());
		INITIAL_COLUMNS.add(ratingColumn);
		Column yearColumn = new Column();
		yearColumn.setColumnType(ColumnType.YEAR);
		yearColumn.setWidth(ColumnType.YEAR.getDefaultWidth());
		INITIAL_COLUMNS.add(yearColumn);
	}

	private List<Column> columns = INITIAL_COLUMNS;

	@OneToMany(cascade = CascadeType.ALL)
	public List<Column> getColumns() {
		if (columns == null) {
			columns = new ArrayList<Column>();
		}
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

}
