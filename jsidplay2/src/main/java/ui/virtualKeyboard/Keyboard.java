package ui.virtualKeyboard;

import java.util.HashSet;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import sidplay.Player;
import ui.common.C64Window;

public class Keyboard extends C64Window {

	@FXML
	private ToggleButton shiftLocked;

	private final Set<KeyTableEntry> keysPressed = new HashSet<>();

	public Keyboard() {
		super();
	}

	public Keyboard(Player player) {
		super(player);
	}

	@Override
	protected void initialize() {
		getStage().resizableProperty().set(false);
	}

	@Override
	public void doClose() {
		for (KeyTableEntry keyTableEntry : getC64().getKeyboard().getKeysDown()) {
			releaseC64Key(keyTableEntry);
		}
	}

	@FXML
	private void keyPressed(MouseEvent mouseEvent) {
		ToggleButton button = (ToggleButton) mouseEvent.getSource();
		KeyTableEntry keyTableEntry = Enum.valueOf(KeyTableEntry.class, button.getUserData().toString());

		if (!shiftLocked.equals(button) && mouseEvent.getButton() == MouseButton.PRIMARY) {
			button.setSelected(true);
			pressC64Key(keyTableEntry);
		}
		if (shiftLocked.equals(button) || mouseEvent.getButton() == MouseButton.SECONDARY) {
			if (!button.isSelected()) {
				keysPressed.add(keyTableEntry);
			}
		}
	}

	@FXML
	private void keyReleased(MouseEvent mouseEvent) {
		ToggleButton button = (ToggleButton) mouseEvent.getSource();
		KeyTableEntry keyTableEntry = Enum.valueOf(KeyTableEntry.class, button.getUserData().toString());

		if (!shiftLocked.equals(button) && mouseEvent.getButton() == MouseButton.PRIMARY) {
			button.setSelected(false);
			releaseC64Key(keyTableEntry);
		}
		if (shiftLocked.equals(button) || mouseEvent.getButton() == MouseButton.SECONDARY) {
			if (keysPressed.remove(keyTableEntry)) {
				button.setSelected(true);
				pressC64Key(keyTableEntry);
			} else {
				button.setSelected(false);
				releaseC64Key(keyTableEntry);
			}
		}
	}

	@FXML
	private void restore() {
		util.getPlayer().getC64().getKeyboard().restore();
	}

	private void pressC64Key(final KeyTableEntry key) {
		getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	private void releaseC64Key(final KeyTableEntry key) {
		getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyReleased(key);
					}
				});
	}

	@FXML
	private void list() {
		util.getPlayer().typeInCommand("LIST\r");
	}

	@FXML
	private void run() {
		util.getPlayer().typeInCommand("RUN\r");
	}

	@FXML
	private void sys() {
		util.getPlayer().typeInCommand("SYS 49152");
	}

	@FXML
	private void reset() {
		util.getPlayer().typeInCommand("SYS 64738\r");
	}

	@FXML
	private void directory() {
		util.getPlayer().typeInCommand("LOAD\"$\",8\r");
	}

	@FXML
	private void diskLoad() {
		util.getPlayer().typeInCommand("LOAD\"*\",8,1\r");
	}

	@FXML
	private void tapeLoad() {
		util.getPlayer().typeInCommand("LOAD\r");
	}

	protected C64 getC64() {
		return util.getPlayer().getC64();
	}

}
