package ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import mp3.ID3Tag;

public class Lame extends UIStage {
	private static final URL DEFAULT_PICTURE = Lame.class
			.getResource("picture.png");

	@FXML
	private ImageView cover;
	@FXML
	private Button doRemove, doEncodeDecode, saveTags;
	@FXML
	private TableView<ConversionTask> files;
	@FXML
	private TableColumn<ConversionTask, Double> progressColumn;

	@FXML
	private ComboBox<String> presets, vbr, algorithm;
	@FXML
	private ComboBox<Integer> cbr, abr;
	@FXML
	private RadioButton setCBR, setABR, setVBR;
	@FXML
	private RadioButton stereo, jointStereo, forcedJointStereo, dualChannels,
			mono, auto;
	@FXML
	private RadioButton outputIsInput, customOutputDir;
	@FXML
	private TextField outputDir;
	@FXML
	private CheckBox overwrite;
	@FXML
	private ComboBox<String> title, artist, album, year, track, genre, comment;

	private ObservableList<ConversionTask> convertableFiles = FXCollections
			.<ConversionTask> observableArrayList();
	private ObservableList<String> presetsList = FXCollections
			.<String> observableArrayList();
	private ObservableList<Integer> cbrList = FXCollections
			.<Integer> observableArrayList();
	private ObservableList<String> vbrList = FXCollections
			.<String> observableArrayList();
	private ObservableList<Integer> abrList = FXCollections
			.<Integer> observableArrayList();
	private ObservableList<String> algorithmList = FXCollections
			.<String> observableArrayList();
	private ObservableList<String> genreList = FXCollections
			.<String> observableArrayList();

	private String coverFilename;
	private File lastDir;
	private ExtensionFilter musicFilter = new ExtensionFilter(
			"MUSIC (WAV, MP3)", "*.wav", "*.mp3");

	@FXML
	private void add() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(lastDir);
		fileDialog.getExtensionFilters().add(musicFilter);
		final List<File> selectedFiles = fileDialog
				.showOpenMultipleDialog(files.getScene().getWindow());
		if (selectedFiles != null) {
			lastDir = selectedFiles.get(0).getParentFile();
			for (File file : selectedFiles) {
				ConversionTask convertableFile = new ConversionTask();
				convertableFile.setNo(convertableFiles.size());
				convertableFile.setFile(file);
				convertableFiles.add(convertableFile);
			}
		}
	}

	@FXML
	private void remove() {
		convertableFiles
				.removeAll(files.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void setPresets() {
		setPresetsOrCustom(true);
	}

	@FXML
	private void setCustom() {
		setPresetsOrCustom(false);
	}

	@FXML
	private void encodeDecode() {
		for (ConversionTask service : files.getItems()) {
			try {
				service.setCmd(getCommand(service.getFile()));
				service.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void chooseOutputDir() {
		final DirectoryChooser fileDialog = new DirectoryChooser();
		fileDialog.setInitialDirectory(lastDir);
		final File selectedFile = fileDialog.showDialog(files.getScene()
				.getWindow());
		if (selectedFile != null) {
			lastDir = selectedFile.getParentFile();
			outputDir.setText(selectedFile.getAbsolutePath());
			customOutputDir.setSelected(true);
		}
	}

	@FXML
	private void setPicture() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(lastDir);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter("JPG file", "*.jpg"));
		final File selectedFile = fileDialog.showOpenDialog(files.getScene()
				.getWindow());
		coverFilename = null;
		cover.setImage(new Image(DEFAULT_PICTURE.toString()));
		if (selectedFile != null) {
			lastDir = selectedFile.getParentFile();
			try {
				coverFilename = selectedFile.getAbsolutePath();
				cover.setImage(new Image(selectedFile.toURI().toURL()
						.toString()));
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	private void doSaveTags() {
		for (ConversionTask service : files.getItems()) {
			if (!"mp3".equals(service.getType())) {
				continue;
			}
			File file = service.getFile();
			File newFile = new File(file.getParentFile(), file.getName()
					.substring(0, file.getName().lastIndexOf('.')) + ".new.mp3");
			try {
				service.setCmd(getSaveTagsCommand(file, newFile));
				service.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		progressColumn.setCellFactory(ProgressBarTableCell
				.<ConversionTask> forTableColumn());

		files.setItems(convertableFiles);
		presets.setItems(presetsList);
		cbr.setItems(cbrList);
		vbr.setItems(vbrList);
		abr.setItems(abrList);
		algorithm.setItems(algorithmList);
		genre.setItems(genreList);

		presetsList.addAll(getBundle().getString("PRESET_MEDIUM"), getBundle()
				.getString("PRESET_STANDARD"),
				getBundle().getString("PRESET_EXTREME"),
				getBundle().getString("PRESET_INSANE"),
				getBundle().getString("PRESET_STANDARD"));
		presets.getSelectionModel().select(
				getBundle().getString("PRESET_STANDARD"));

		cbrList.addAll(320, 256, 224, 192, 160, 128, 112, 96, 80, 64, 56, 48,
				40, 32, 192);
		cbr.getSelectionModel().select(Integer.valueOf(192));

		vbrList.addAll(getBundle().getString("VBR_0"),
				getBundle().getString("VBR_1"), getBundle().getString("VBR_2"),
				getBundle().getString("VBR_3"), getBundle().getString("VBR_4"),
				getBundle().getString("VBR_5"), getBundle().getString("VBR_6"),
				getBundle().getString("VBR_7"), getBundle().getString("VBR_8"),
				getBundle().getString("VBR_9"));
		vbr.getSelectionModel().select(getBundle().getString("VBR_2"));

		for (int i = 310; i >= 8; i--) {
			abrList.add(i);
		}
		abr.getSelectionModel().select(Integer.valueOf(192));

		algorithmList.addAll(getBundle().getString("ALG_0"), getBundle()
				.getString("ALG_1"), getBundle().getString("ALG_2"),
				getBundle().getString("ALG_3"), getBundle().getString("ALG_4"),
				getBundle().getString("ALG_5"), getBundle().getString("ALG_6"),
				getBundle().getString("ALG_7"), getBundle().getString("ALG_8"),
				getBundle().getString("ALG_9"),
				getBundle().getString("ALG_AUTO"));
		algorithm.getSelectionModel().select(getBundle().getString("ALG_2"));

		genreList.add("");
		ID3Tag id3 = new ID3Tag();
		id3.id3tag_genre_list((int num, String name) -> genreList.add(name));

		files.getSelectionModel()
				.selectedItemProperty()
				.addListener(
						(observable, oldValue, newValue) -> saveTags
								.setDisable(files.getSelectionModel()
										.getSelectedItems().size() == 0)

				);
		cover.setImage(new Image(DEFAULT_PICTURE.toString()));
	}

	private void setPresetsOrCustom(boolean isPresets) {
		presets.setDisable(!isPresets);
		setCBR.setDisable(isPresets);
		setABR.setDisable(isPresets);
		setVBR.setDisable(isPresets);
		cbr.setDisable(isPresets);
		abr.setDisable(isPresets);
		vbr.setDisable(isPresets);
	}

	private ArrayList<String> getSaveTagsCommand(final File file,
			final File newFile) throws IOException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("--space-id3v1");
		if (title.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--tt");
			cmd.add(title.getSelectionModel().getSelectedItem());
		}
		if (artist.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--ta");
			cmd.add(artist.getSelectionModel().getSelectedItem());
		}
		if (album.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--tl");
			cmd.add(album.getSelectionModel().getSelectedItem());
		}
		if (year.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--ty");
			cmd.add(year.getSelectionModel().getSelectedItem());
		}
		if (track.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--tn");
			cmd.add(track.getSelectionModel().getSelectedItem());
		}
		if (genre.getSelectionModel().getSelectedItem().length() > 0) {
			cmd.add("--tg");
			cmd.add(genre.getSelectionModel().getSelectedItem());
		}
		if (comment.getSelectionModel().getSelectedItem() != null) {
			cmd.add("--tc");
			cmd.add(comment.getSelectionModel().getSelectedItem());
		}
		if (coverFilename != null) {
			cmd.add("--ti");
			cmd.add(coverFilename);
		}
		cmd.add(file.getAbsolutePath());
		cmd.add(newFile.getAbsolutePath());
		return cmd;
	}

	private ArrayList<String> getCommand(File file) throws IOException {
		ArrayList<String> cmd = new ArrayList<String>();
		if (file.getName().toLowerCase(Locale.US).endsWith(".mp3")) {
			cmd.add("--decode");
		}
		cmd.add("--embedded");
		cmd.add("--silent");
		if (!presets.isDisabled()) {
			// preset
			String pr = String.valueOf(presets.getSelectionModel()
					.getSelectedItem());
			cmd.add("--preset");
			cmd.add(pr.substring(0, pr.indexOf(' ')).toLowerCase(Locale.US));
		} else {
			// custom
			if (setVBR.isSelected()) {
				// vbr
				String v = String.valueOf(vbr.getSelectionModel()
						.getSelectedItem());
				if (v.indexOf(' ') != -1) {
					v = v.substring(0, v.indexOf(' '));
				}
				cmd.add("-v");
				cmd.add("-V");
				cmd.add(v);
			} else if (setABR.isSelected()) {
				// abr
				String a = String.valueOf(abr.getSelectionModel()
						.getSelectedItem());
				cmd.add("--abr");
				cmd.add(a);
			} else {
				// cbr
				String a = String.valueOf(cbr.getSelectionModel()
						.getSelectedItem());
				cmd.add("--cbr");
				cmd.add("-b");
				cmd.add(a);
			}
		}
		String eaq = String.valueOf(algorithm.getSelectionModel()
				.getSelectedItem());
		if (!"Auto".equals(eaq)) {
			if (eaq.indexOf(' ') != -1) {
				eaq = eaq.substring(0, eaq.indexOf(' '));
			}
			cmd.add("-q");
			cmd.add(eaq);
		}

		if (stereo.isSelected()) {
			cmd.add("-m");
			cmd.add("s");
		} else if (jointStereo.isSelected()) {
			cmd.add("-m");
			cmd.add("j");
		} else if (forcedJointStereo.isSelected()) {
			cmd.add("-m");
			cmd.add("f");
		} else if (dualChannels.isSelected()) {
			cmd.add("-m");
			cmd.add("d");
		} else if (mono.isSelected()) {
			cmd.add("-m");
			cmd.add("m");
		}

		cmd.add(file.getAbsolutePath());

		String outDir;
		String outName = file.getName();
		if (outName.toLowerCase(Locale.US).endsWith(".mp3")) {
			outName = outName.substring(0, outName.lastIndexOf('.')) + ".wav";
		} else {
			outName = outName.substring(0, outName.lastIndexOf('.')) + ".mp3";
		}
		if (!outputIsInput.isSelected()) {
			outDir = outputDir.getText();
			if (outDir.length() == 0) {
				outDir = System.getProperty("user.dir");
			}
		} else {
			// Custom output dir
			outDir = file.getParent();
		}
		cmd.add(new File(outDir, outName).getAbsolutePath());
		if (!overwrite.isSelected() && new File(outDir, outName).exists()) {
			throw new IOException("Output file " + new File(outDir, outName)
					+ " already exists!");
		}
		return cmd;
	}

}
