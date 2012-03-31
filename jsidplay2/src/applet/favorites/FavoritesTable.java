package applet.favorites;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
public class FavoritesTable extends JTable {

	public FavoritesTable(TableModel dm) {
		super(dm);
		// disable enter key behavior
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		FavoritesCellRenderer favoritesCellRenderer = new FavoritesCellRenderer(
				(FavoritesModel) dm);
		setDefaultRenderer(Object.class, favoritesCellRenderer);
		setDefaultRenderer(String.class, favoritesCellRenderer);
		setDefaultRenderer(Integer.class, favoritesCellRenderer);
	}
	
}
