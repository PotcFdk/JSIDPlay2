package ui.virtualKeyboard;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import ui.common.C64Stage;

public class Keyboard extends C64Stage {

	@FXML
	private Button arrowLeft, one, two, three, four, five, six, seven, eight,
			nine, zero, plus, minus, pound, clrHome, insDel, q, w, e, r, t, y,
			u, i, o, p, at, asterisk, arrowUp, restore, a, s, d, f, g, h, j, k,
			l, colon, semicolon, equals, ret, z, x, c, v, b, n, m, comma,
			period, slash, cursorUpDown, cursorLeftRight, space, f1, f3, f5,
			f7;
	@FXML
	private ToggleButton runStop, shift, commodore, shiftLock, ctrl,
			rightShift;

	@FXML
	private void arrowLeft() {
		pressC64Key(KeyTableEntry.ARROW_LEFT);
		releaseC64Key(KeyTableEntry.ARROW_LEFT);
	}

	@FXML
	private void one() {
		pressC64Key(KeyTableEntry.ONE);
		releaseC64Key(KeyTableEntry.ONE);
	}

	@FXML
	private void two() {
		pressC64Key(KeyTableEntry.TWO);
		releaseC64Key(KeyTableEntry.TWO);
	}

	@FXML
	private void three() {
		pressC64Key(KeyTableEntry.THREE);
		releaseC64Key(KeyTableEntry.THREE);
	}

	@FXML
	private void four() {
		pressC64Key(KeyTableEntry.FOUR);
		releaseC64Key(KeyTableEntry.FOUR);
	}

	@FXML
	private void five() {
		pressC64Key(KeyTableEntry.FIVE);
		releaseC64Key(KeyTableEntry.FIVE);
	}

	@FXML
	private void six() {
		pressC64Key(KeyTableEntry.SIX);
		releaseC64Key(KeyTableEntry.SIX);
	}

	@FXML
	private void seven() {
		pressC64Key(KeyTableEntry.SEVEN);
		releaseC64Key(KeyTableEntry.SEVEN);
	}

	@FXML
	private void eight() {
		pressC64Key(KeyTableEntry.EIGHT);
		releaseC64Key(KeyTableEntry.EIGHT);
	}

	@FXML
	private void nine() {
		pressC64Key(KeyTableEntry.NINE);
		releaseC64Key(KeyTableEntry.NINE);
	}

	@FXML
	private void zero() {
		pressC64Key(KeyTableEntry.ZERO);
		releaseC64Key(KeyTableEntry.ZERO);
	}

	@FXML
	private void plus() {
		pressC64Key(KeyTableEntry.PLUS);
		releaseC64Key(KeyTableEntry.PLUS);
	}

	@FXML
	private void minus() {
		pressC64Key(KeyTableEntry.MINUS);
		releaseC64Key(KeyTableEntry.MINUS);
	}

	@FXML
	private void pound() {
		pressC64Key(KeyTableEntry.POUND);
		releaseC64Key(KeyTableEntry.POUND);
	}

	@FXML
	private void clrHome() {
		pressC64Key(KeyTableEntry.CLEAR_HOME);
		releaseC64Key(KeyTableEntry.CLEAR_HOME);
	}

	@FXML
	private void insDel() {
		pressC64Key(KeyTableEntry.INS_DEL);
		releaseC64Key(KeyTableEntry.INS_DEL);
	}

	@FXML
	private void ctrl() {
		if (ctrl.isSelected()) {
			pressC64Key(KeyTableEntry.CTRL);
		} else {
			releaseC64Key(KeyTableEntry.CTRL);
		}
	}

	@FXML
	private void q() {
		pressC64Key(KeyTableEntry.Q);
		releaseC64Key(KeyTableEntry.Q);
	}

	@FXML
	private void w() {
		pressC64Key(KeyTableEntry.W);
		releaseC64Key(KeyTableEntry.W);
	}

	@FXML
	private void e() {
		pressC64Key(KeyTableEntry.E);
		releaseC64Key(KeyTableEntry.E);
	}

	@FXML
	private void r() {
		pressC64Key(KeyTableEntry.R);
		releaseC64Key(KeyTableEntry.R);
	}

	@FXML
	private void t() {
		pressC64Key(KeyTableEntry.T);
		releaseC64Key(KeyTableEntry.T);
	}

	@FXML
	private void y() {
		pressC64Key(KeyTableEntry.Y);
		releaseC64Key(KeyTableEntry.Y);
	}

	@FXML
	private void u() {
		pressC64Key(KeyTableEntry.U);
		releaseC64Key(KeyTableEntry.U);
	}

	@FXML
	private void i() {
		pressC64Key(KeyTableEntry.I);
		releaseC64Key(KeyTableEntry.I);
	}

	@FXML
	private void o() {
		pressC64Key(KeyTableEntry.O);
		releaseC64Key(KeyTableEntry.O);
	}

	@FXML
	private void p() {
		pressC64Key(KeyTableEntry.P);
		releaseC64Key(KeyTableEntry.P);
	}

	@FXML
	private void at() {
		pressC64Key(KeyTableEntry.AT);
		releaseC64Key(KeyTableEntry.AT);
	}

	@FXML
	private void asterisk() {
		pressC64Key(KeyTableEntry.STAR);
		releaseC64Key(KeyTableEntry.STAR);
	}

	@FXML
	private void arrowUp() {
		pressC64Key(KeyTableEntry.ARROW_UP);
		releaseC64Key(KeyTableEntry.ARROW_UP);
	}

	@FXML
	private void restore() {
		getPlayer().getC64().getKeyboard().restore();
	}

	@FXML
	private void runStop() {
		if (runStop.isSelected()) {
			pressC64Key(KeyTableEntry.RUN_STOP);
		} else {
			releaseC64Key(KeyTableEntry.RUN_STOP);
		}
	}

	@FXML
	private void shiftLock() {
		if (shiftLock.isSelected()) {
			pressC64Key(KeyTableEntry.SHIFT_LEFT);
		} else {
			releaseC64Key(KeyTableEntry.SHIFT_LEFT);
		}
	}

	@FXML
	private void a() {
		pressC64Key(KeyTableEntry.A);
		releaseC64Key(KeyTableEntry.A);
	}

	@FXML
	private void s() {
		pressC64Key(KeyTableEntry.S);
		releaseC64Key(KeyTableEntry.S);
	}

	@FXML
	private void d() {
		pressC64Key(KeyTableEntry.D);
		releaseC64Key(KeyTableEntry.D);
	}

	@FXML
	private void f() {
		pressC64Key(KeyTableEntry.F);
		releaseC64Key(KeyTableEntry.F);
	}

	@FXML
	private void g() {
		pressC64Key(KeyTableEntry.G);
		releaseC64Key(KeyTableEntry.G);
	}

	@FXML
	private void h() {
		pressC64Key(KeyTableEntry.H);
		releaseC64Key(KeyTableEntry.H);
	}

	@FXML
	private void j() {
		pressC64Key(KeyTableEntry.J);
		releaseC64Key(KeyTableEntry.J);
	}

	@FXML
	private void k() {
		pressC64Key(KeyTableEntry.K);
		releaseC64Key(KeyTableEntry.K);
	}

	@FXML
	private void l() {
		pressC64Key(KeyTableEntry.L);
		releaseC64Key(KeyTableEntry.L);
	}

	@FXML
	private void colon() {
		pressC64Key(KeyTableEntry.COLON);
		releaseC64Key(KeyTableEntry.COLON);
	}

	@FXML
	private void semicolon() {
		pressC64Key(KeyTableEntry.SEMICOLON);
		releaseC64Key(KeyTableEntry.SEMICOLON);
	}

	@FXML
	private void equals() {
		pressC64Key(KeyTableEntry.EQUALS);
		releaseC64Key(KeyTableEntry.EQUALS);
	}

	@FXML
	private void ret() {
		pressC64Key(KeyTableEntry.RETURN);
		releaseC64Key(KeyTableEntry.RETURN);
	}

	@FXML
	private void commodore() {
		if (commodore.isSelected()) {
			pressC64Key(KeyTableEntry.COMMODORE);
		} else {
			releaseC64Key(KeyTableEntry.COMMODORE);
		}
	}

	@FXML
	private void shift() {
		if (shift.isSelected()) {
			pressC64Key(KeyTableEntry.SHIFT_LEFT);
		} else {
			releaseC64Key(KeyTableEntry.SHIFT_LEFT);
		}
	}

	@FXML
	private void z() {
		pressC64Key(KeyTableEntry.Z);
		releaseC64Key(KeyTableEntry.Z);
	}

	@FXML
	private void x() {
		pressC64Key(KeyTableEntry.X);
		releaseC64Key(KeyTableEntry.X);
	}

	@FXML
	private void c() {
		pressC64Key(KeyTableEntry.C);
		releaseC64Key(KeyTableEntry.C);
	}

	@FXML
	private void v() {
		pressC64Key(KeyTableEntry.V);
		releaseC64Key(KeyTableEntry.V);
	}

	@FXML
	private void b() {
		pressC64Key(KeyTableEntry.B);
		releaseC64Key(KeyTableEntry.B);
	}

	@FXML
	private void n() {
		pressC64Key(KeyTableEntry.N);
		releaseC64Key(KeyTableEntry.N);
	}

	@FXML
	private void m() {
		pressC64Key(KeyTableEntry.M);
		releaseC64Key(KeyTableEntry.M);
	}

	@FXML
	private void comma() {
		pressC64Key(KeyTableEntry.COMMA);
		releaseC64Key(KeyTableEntry.COMMA);
	}

	@FXML
	private void period() {
		pressC64Key(KeyTableEntry.PERIOD);
		releaseC64Key(KeyTableEntry.PERIOD);
	}

	@FXML
	private void slash() {
		pressC64Key(KeyTableEntry.SLASH);
		releaseC64Key(KeyTableEntry.SLASH);
	}

	@FXML
	private void rightShift() {
		if (rightShift.isSelected()) {
			pressC64Key(KeyTableEntry.SHIFT_RIGHT);
		} else {
			releaseC64Key(KeyTableEntry.SHIFT_RIGHT);
		}
	}

	@FXML
	private void cursorUpDown() {
		pressC64Key(KeyTableEntry.CURSOR_UP_DOWN);
		releaseC64Key(KeyTableEntry.CURSOR_UP_DOWN);
	}

	@FXML
	private void cursorLeftRight() {
		pressC64Key(KeyTableEntry.CURSOR_LEFT_RIGHT);
		releaseC64Key(KeyTableEntry.CURSOR_LEFT_RIGHT);
	}

	@FXML
	private void space() {
		pressC64Key(KeyTableEntry.SPACE);
		releaseC64Key(KeyTableEntry.SPACE);
	}

	@FXML
	private void f1() {
		pressC64Key(KeyTableEntry.F1);
		releaseC64Key(KeyTableEntry.F1);
	}

	@FXML
	private void f3() {
		pressC64Key(KeyTableEntry.F3);
		releaseC64Key(KeyTableEntry.F3);
	}

	@FXML
	private void f5() {
		pressC64Key(KeyTableEntry.F5);
		releaseC64Key(KeyTableEntry.F5);
	}

	@FXML
	private void f7() {
		pressC64Key(KeyTableEntry.F7);
		releaseC64Key(KeyTableEntry.F7);
	}

	private void pressC64Key(final KeyTableEntry key) {
		getC64().getEventScheduler().scheduleThreadSafe(
				new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	private void releaseC64Key(final KeyTableEntry key) {
		getC64().getEventScheduler().scheduleThreadSafe(
				new Event("Virtual Keyboard Key Released: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						getC64().getKeyboard().keyReleased(key);
					}
				});
	}

	private C64 getC64() {
		return getPlayer().getC64();
	}

}
