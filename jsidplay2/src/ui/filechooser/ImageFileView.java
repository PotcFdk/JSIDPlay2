package ui.filechooser;

import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

import ui.JSIDPlay2Main;
import ui.filefilter.TuneFileFilter;


// TODO Add custom icons for file types.
public class ImageFileView extends FileView {
	private static final ImageIcon PLAY_ICON = new ImageIcon(
			JSIDPlay2Main.class.getResource("icons/play.png"));

	private TuneFileFilter tuneFileFilter = new TuneFileFilter();
	
	public String getName(File f) {
		return null; // let the L&F FileView figure this out
	}

	public String getDescription(File f) {
		return null; // let the L&F FileView figure this out
	}

	public Boolean isTraversable(File f) {
		return null; // let the L&F FileView figure this out
	}

	public String getTypeDescription(File file) {
		if (tuneFileFilter.accept(file)) {
			return "SID tune";
		}
		return null;
	}

	public Icon getIcon(File file) {
		if (file.isFile() && tuneFileFilter.accept(file)
				&& !file.getName().toLowerCase().endsWith(".zip")) {
			return PLAY_ICON;
		}
		return null;
	}

}