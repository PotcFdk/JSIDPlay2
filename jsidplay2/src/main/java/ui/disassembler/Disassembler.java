package ui.disassembler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import libsidplay.Player;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.cpuparser.CPUCommand;
import libsidutils.cpuparser.CPUParser;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.State;
import ui.common.C64Stage;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;

public class Disassembler extends C64Stage {

	private final class DisassemblerRefresh implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable,
				State oldValue, State newValue) {
			if (newValue == State.RUNNING) {
				Platform.runLater(() -> setTune());
			}
		}
	}

	@FXML
	private TextField address, startAddress, endAddress;
	@FXML
	private Button driverAddress, loadAddress, initAddress, playerAddress,
			save;
	@FXML
	private TableView<AssemblyLine> memoryTable;

	private ObservableList<AssemblyLine> assemblyLines;

	private DisassemblerRefresh disassemblerRefresh;

	private static final Map<Integer, CPUCommand> fCommands = CPUParser
			.getCpuCommands();

	public Disassembler(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
	}

	@FXML
	private void initialize() {
		assemblyLines = FXCollections.<AssemblyLine> observableArrayList();
		memoryTable.setItems(assemblyLines);
		disassemble(0);
		setTune();

		disassemblerRefresh = new DisassemblerRefresh();
		util.getConsolePlayer().stateProperty()
				.addListener(disassemblerRefresh);

	}

	@Override
	public void doClose() {
		util.getConsolePlayer().stateProperty()
				.removeListener(disassemblerRefresh);
	}

	protected void setTune() {
		if (util.getPlayer().getTune() == null) {
			return;
		}
		Platform.runLater(() -> {
			final SidTuneInfo tuneInfo = util.getPlayer().getTune().getInfo();
			driverAddress.setText(getDriverAddress(tuneInfo));
			loadAddress.setText(getLoadAddress(tuneInfo));
			initAddress.setText(getInitAddress(tuneInfo));
			playerAddress.setText(getPlayerAddress(tuneInfo));
		});
	}

	private String getPlayerAddress(final SidTuneInfo tuneInfo) {
		if (tuneInfo.playAddr == 0xffff) {
			return "N/A";
		} else {
			return String.format("0x%04x", tuneInfo.playAddr);
		}
	}

	private String getInitAddress(final SidTuneInfo tuneInfo) {
		if (tuneInfo.playAddr == 0xffff) {
			return String.format("0x%04x", tuneInfo.initAddr);
		} else {
			return String.format("0x%04x", tuneInfo.initAddr);
		}
	}

	private String getLoadAddress(final SidTuneInfo tuneInfo) {
		return String.format("0x%04x - 0x%04x", tuneInfo.loadAddr,
				tuneInfo.loadAddr + tuneInfo.c64dataLen - 1);
	}

	private String getDriverAddress(final SidTuneInfo tuneInfo) {
		if (tuneInfo.determinedDriverAddr == 0) {
			return "N/A";
		} else {
			return String.format("0x%04x - 0x%04x",
					tuneInfo.determinedDriverAddr,
					tuneInfo.determinedDriverAddr
							+ tuneInfo.determinedDriverLength - 1);
		}
	}

	private void disassemble(final int startAddr) {
		Platform.runLater(() -> {
			assemblyLines.clear();
			byte[] ram = util.getPlayer().getC64().getRAM();
			int offset = startAddr;
			do {
				CPUCommand cmd = fCommands.get(ram[offset & 0xffff] & 0xff);
				AssemblyLine assemblyLine = createAssemblyLine(ram,
						offset & 0xffff, cmd);
				assemblyLines.add(assemblyLine);
				offset += cmd.getByteCount();
			} while (offset <= 0xffff);
		});
	}

	protected AssemblyLine createAssemblyLine(byte[] ram, int startAddr,
			CPUCommand cmd) {
		AssemblyLine assemblyLine = new AssemblyLine();
		assemblyLine.setAddress(String.format("%04X", startAddr & 0xffff));
		StringBuilder hexBytes = new StringBuilder();
		for (int i = 0; i < cmd.getByteCount(); i++) {
			hexBytes.append(String.format("%02X ", ram[startAddr + i & 0xffff]));
		}
		assemblyLine.setBytes(hexBytes.toString());
		String[] parts = CPUParser.getInstance().getDisassembly(ram, startAddr)
				.split(":", 2);
		assemblyLine.setMnemonic(parts[0]);
		if (parts.length == 2) {
			assemblyLine.setOperands(parts[1]);
		}
		assemblyLine.setCycles(cmd.getCycles());
		return assemblyLine;
	}

	@FXML
	private void gotoMemStart() {
		disassemble(0);
	}

	@FXML
	private void gotoAddress() {
		gotoMem(address.getText());
	}

	@FXML
	private void gotoDriverAddress() {
		gotoMem(driverAddress.getText());
	}

	@FXML
	private void gotoLoadAddress() {
		gotoMem(loadAddress.getText());
	}

	@FXML
	private void gotoInitAddress() {
		gotoMem(initAddress.getText());
	}

	@FXML
	private void gotoPlayerAddress() {
		gotoMem(playerAddress.getText());
	}

	private void gotoMem(final String destination) {
		try {
			disassemble(Integer.decode(destination.substring(0, 6)) & 0xffff);
		} catch (final NumberFormatException e) {
		}
	}

	@FXML
	private void saveMemory() {
		FileChooser fileDialog = new FileChooser();
		SidPlay2Section sidplay2 = (SidPlay2Section) util.getConfig()
				.getSidplay2();
		fileDialog.setInitialDirectory(sidplay2.getLastDirectoryFolder());
		File file = fileDialog.showSaveDialog(save.getScene().getWindow());
		if (file != null) {
			sidplay2.setLastDirectory(file.getParentFile().getAbsolutePath());
			try (FileOutputStream fos = new FileOutputStream(file)) {
				int start = Integer.decode(startAddress.getText());
				int end = Integer.decode(endAddress.getText()) + 1;
				start &= 0xffff;
				end &= 0xffff;
				fos.write(start & 0xff);
				fos.write(start >> 8);
				byte[] ram = util.getPlayer().getC64().getRAM();
				while (start != end) {
					fos.write(ram[start]);
					start = start + 1 & 0xffff;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
