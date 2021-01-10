package ui.entities.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import ui.common.properties.LazyListField;

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
		Assembly64Column yearColumn = new Assembly64Column();
		yearColumn.setColumnType(Assembly64ColumnType.RELEASED);
		yearColumn.setWidth(Assembly64ColumnType.RELEASED.getDefaultWidth());
		INITIAL_COLUMNS.add(yearColumn);
		Assembly64Column ratingColumn = new Assembly64Column();
		ratingColumn.setColumnType(Assembly64ColumnType.RATING);
		ratingColumn.setWidth(Assembly64ColumnType.RATING.getDefaultWidth());
		INITIAL_COLUMNS.add(ratingColumn);
	}

	private LazyListField<Assembly64Column> columns = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	public List<Assembly64Column> getColumns() {
		return columns.get(INITIAL_COLUMNS);
	}

	public void setColumns(List<Assembly64Column> columns) {
		this.columns.set(columns);
	}

}
