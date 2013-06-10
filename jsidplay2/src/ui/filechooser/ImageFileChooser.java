package ui.filechooser;

import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ui.entities.config.Configuration;
import ui.filechooser.zip.ZipFileSystemView;

import libsidutils.zip.ZipEntryFileProxy;

public class ImageFileChooser extends JFileChooser implements
		PropertyChangeListener {

	private Configuration config;
	private File autostartFile;

	public ImageFileChooser(final Configuration cfg, File hvscRoot,
			final FileFilter filter) {
		super(cfg.getSidplay2().getLastDirectory(), new ZipFileSystemView());
		this.config = cfg;
		setFileFilter(filter);
		setFileView(new ImageFileView());
		final ImagePreview imagePreview = new ImagePreview(hvscRoot);
		imagePreview.setConfig(cfg);
		imagePreview.addPropertyChangeListener(this);
		addPropertyChangeListener(imagePreview);
		setAccessory(imagePreview);
	}

	@Override
	public int showDialog(Component parent, String approveButtonText)
			throws HeadlessException {
		int rc = super.showDialog(parent, approveButtonText);
		try {
			if (rc == JFileChooser.APPROVE_OPTION && getSelectedFile() != null) {
				final File selectedFile = getSelectedFile();
				if (selectedFile instanceof ZipEntryFileProxy) {
					// Load file entry from ZIP
					ZipEntryFileProxy zipEntry = (ZipEntryFileProxy) selectedFile;
					setSelectedFile(ZipEntryFileProxy.extractFromZip(zipEntry,
							config.getSidplay2().getTmpDir()));
					config.getSidplay2().setLastDirectory(
							zipEntry.getZip().getAbsolutePath());
				} else {
					config.getSidplay2().setLastDirectory(
							selectedFile.getParentFile().getAbsolutePath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rc;
	}

	public void setAutoStartFile(File file) {
		autostartFile = file;
	}

	public File getAutostartFile() {
		return autostartFile;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (ImagePreview.PROP_AUTOSTART_PRG.equals(evt.getPropertyName())) {
			File file = (File) evt.getNewValue();
			setAutoStartFile(file);
			// Insert media
			approveSelection();
		} else if (ImagePreview.PROP_ATTACH_IMAGE.equals(evt.getPropertyName())) {
			setAutoStartFile(null);
		}
	}

}
