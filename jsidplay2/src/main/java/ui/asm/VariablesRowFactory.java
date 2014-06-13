package ui.asm;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class VariablesRowFactory implements
		Callback<TableView<Variable>, TableRow<Variable>> {
	@Override
	public TableRow<Variable> call(final TableView<Variable> p) {
		return new TableRow<Variable>() {
			@Override
			public void updateItem(Variable item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(item != null ? new Tooltip(getItem().getName() + "="
						+ getItem().getValue()) : null);
			}
		};
	}
}
