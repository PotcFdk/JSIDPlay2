package libsidutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class Extract7Zip {

	private class ExtractCallback implements IArchiveExtractCallback {
		private IInArchive inArchive;
		private OutputStream outputStream;
		private File file;
		private boolean isFolder;

		private ExtractCallback(IInArchive inArchive) {
			this.inArchive = inArchive;
		}

		@Override
		public void setTotal(long total) throws SevenZipException {

		}

		@Override
		public void setCompleted(long completeValue) throws SevenZipException {

		}

		@Override
		public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (IOException e1) {
					throw new SevenZipException("Error closing file: " + file.getAbsolutePath());
				}
			}

			this.isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);

			if (extractAskMode != ExtractAskMode.EXTRACT) {
				// Skipped files or files being tested
				return null;
			}

			String path = (String) inArchive.getProperty(index, PropID.PATH);
			file = new File(outputDirectoryFile, path);
			if (isFolder) {
				createDirectory(file);
				return null;
			}

			createDirectory(file.getParentFile());
			filesList.add(file);

			try {
				outputStream = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				throw new SevenZipException("Error opening file: " + file.getAbsolutePath(), e);
			}

			return new ISequentialOutStream() {
				public int write(byte[] data) throws SevenZipException {
					try {
						outputStream.write(data);
					} catch (IOException e) {
						throw new SevenZipException("Error writing to file: " + file.getAbsolutePath());
					}
					return data.length; // Return amount of consumed data
				}
			};
		}

		private void createDirectory(File parentFile) throws SevenZipException {
			if (!parentFile.exists()) {
				if (!parentFile.mkdirs()) {
					throw new SevenZipException("Error creating directory: " + parentFile.getAbsolutePath());
				}
			}
		}

		@Override
		public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {

		}

		@Override
		public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (IOException e) {
					throw new SevenZipException("Error closing file: " + file.getAbsolutePath());
				}
			}
		}

	}

	private File archive;
	private File outputDirectoryFile;
	private List<File> filesList = new ArrayList<>();

	public Extract7Zip(File archive, File outputDirectory) {
		this.archive = archive;
		this.outputDirectoryFile = outputDirectory;
	}

	public void extract() throws IOException {
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(archive, "r")) {
			try (IInArchive inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile))) {
				inArchive.extract(null, false, new ExtractCallback(inArchive));
			}
		}
	}

	@SuppressWarnings("serial")
	public File getZipFile() {
		return new File(archive.getAbsolutePath()) {
			@Override
			public File[] listFiles() {
				return filesList.toArray(new File[0]);
			}
		};

	}

}