package applet.filechooser;

import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import libsidutils.zip.ZipEntryFileProxy;

import sidplay.ini.intf.IConfig;
import applet.filechooser.zip.ZipFileSystemView;

public class ImageFileChooser extends JFileChooser implements PropertyChangeListener {

	private IConfig config;
	private File autostartFile;

	public ImageFileChooser(final IConfig cfg, final FileFilter filter) {
		super(cfg.getSidplay2().getLastDirectory(), new ZipFileSystemView());
		this.config = cfg;
		setFileFilter(filter);
		setFileView(new ImageFileView());
		final ImagePreview imagePreview = new ImagePreview();
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
					setSelectedFile(ZipEntryFileProxy.extractFromZip(zipEntry));
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
		}else if (ImagePreview.PROP_ATTACH_IMAGE.equals(evt.getPropertyName())) {
			setAutoStartFile(null);
		}
	}

}
