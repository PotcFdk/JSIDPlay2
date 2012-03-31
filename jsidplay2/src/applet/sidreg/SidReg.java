package applet.sidreg;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import libsidplay.Player;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import applet.events.ITuneStateChanged;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;

public class SidReg extends XDialog implements UIEventListener {

	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	private SwingEngine swix;

	protected JCheckBox startStop, freq1, freq2, freq3, pulse1, pulse2, pulse3,
			ctrl1, ctrl2, ctrl3, ad1, ad2, ad3, sr1, sr2, sr3, filter, vol,
			paddles, osc3, env3;
	protected JButton selectAll, deselectAll;
	protected JTable regTable;

	private final Player player;
	private TableRowSorter<SIDRegModel> sorter;

	public Action doStartStop = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleDataCollection(startStop.isSelected());
		}
	};

	public Action doUpdateFilter = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			updateFilter();
		}
	};

	public Action doSelectAll = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			freq1.setSelected(true);
			freq2.setSelected(true);
			freq3.setSelected(true);
			pulse1.setSelected(true);
			pulse2.setSelected(true);
			pulse3.setSelected(true);
			ctrl1.setSelected(true);
			ctrl2.setSelected(true);
			ctrl3.setSelected(true);
			ad1.setSelected(true);
			ad2.setSelected(true);
			ad3.setSelected(true);
			sr1.setSelected(true);
			sr2.setSelected(true);
			sr3.setSelected(true);
			filter.setSelected(true);
			vol.setSelected(true);
			paddles.setSelected(true);
			osc3.setSelected(true);
			env3.setSelected(true);
			updateFilter();
		}
	};

	public Action doDeselectAll = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			freq1.setSelected(false);
			freq2.setSelected(false);
			freq3.setSelected(false);
			pulse1.setSelected(false);
			pulse2.setSelected(false);
			pulse3.setSelected(false);
			ctrl1.setSelected(false);
			ctrl2.setSelected(false);
			ctrl3.setSelected(false);
			ad1.setSelected(false);
			ad2.setSelected(false);
			ad3.setSelected(false);
			sr1.setSelected(false);
			sr2.setSelected(false);
			sr3.setSelected(false);
			filter.setSelected(false);
			vol.setSelected(false);
			paddles.setSelected(false);
			osc3.setSelected(false);
			env3.setSelected(false);
			updateFilter();
		}
	};

	public SidReg(Player pl) {
		this.player = pl;
		uiEvents.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				uiEvents.removeListener(SidReg.this);
			}
		});
		try {
			swix = new SwingEngine(this);
			swix.insert(SidReg.class.getResource("SidReg.xml"), this);

			fillComboBoxes();
			setDefaultsAndActions();

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));

			sorter = new TableRowSorter<SIDRegModel>(
					(SIDRegModel) regTable.getModel());
			regTable.setRowSorter(sorter);
			((SIDRegModel) regTable.getModel()).setLocalizer(swix
					.getLocalizer());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		// nothing to do
	}

	protected void updateFilter() {
		final List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();
		if (freq1.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_1_FREQ_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_1_FREQ_H")), 2));
		}
		if (freq2.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_2_FREQ_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_2_FREQ_H")), 2));
		}
		if (freq3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_3_FREQ_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_3_FREQ_H")), 2));
		}
		if (pulse1.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_1_PULSE_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_1_PULSE_H")), 2));
		}
		if (pulse2.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_2_PULSE_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_2_PULSE_H")), 2));
		}
		if (pulse3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_3_PULSE_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString(
							"VOICE_3_PULSE_H")), 2));
		}
		if (ctrl1.isSelected()) {
			filters.add(RowFilter.regexFilter(Pattern.quote(swix.getLocalizer()
					.getString("VOICE_1_CTRL")), 2));
		}
		if (ctrl2.isSelected()) {
			filters.add(RowFilter.regexFilter(Pattern.quote(swix.getLocalizer()
					.getString("VOICE_2_CTRL")), 2));
		}
		if (ctrl3.isSelected()) {
			filters.add(RowFilter.regexFilter(Pattern.quote(swix.getLocalizer()
					.getString("VOICE_3_CTRL")), 2));
		}
		if (ad1.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_1_AD")),
					2));
		}
		if (ad2.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_2_AD")),
					2));
		}
		if (ad3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_3_AD")),
					2));
		}
		if (sr1.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_1_SR")),
					2));
		}
		if (sr2.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_2_SR")),
					2));
		}
		if (sr3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("VOICE_3_SR")),
					2));
		}
		if (filter.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("FCUT_L")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("FCUT_H")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("FRES")), 2));
		}
		if (vol.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("FVOL")), 2));
		}
		if (paddles.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("PADDLE1")), 2));
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("PADDLE2")), 2));
		}
		if (osc3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("OSC3")), 2));
		}
		if (env3.isSelected()) {
			filters.add(RowFilter.regexFilter(
					Pattern.quote(swix.getLocalizer().getString("ENV3")), 2));
		}
		final RowFilter<Object, Object> sidRegFilter = RowFilter
				.orFilter(filters);
		sorter.setRowFilter(sidRegFilter);
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	protected void toggleDataCollection(final boolean enable) {
		SIDRegModel regModel = (SIDRegModel) regTable.getModel();
		if (enable) {
			regModel.init();
			player.getC64().setSidWriteListener(0, regModel);
			if (player.getTune() != null
					&& player.getTune().getInfo().sidChipBase2 != 0) {
				player.getC64().setSidWriteListener(1, regModel);
			}
		} else {
			regModel.stop();
			player.getC64().setSidWriteListener(0, null);
			if (player.getTune() != null
					&& player.getTune().getInfo().sidChipBase2 != 0) {
				player.getC64().setSidWriteListener(1, null);
			}
		}
	}

	@Override
	public void notify(UIEvent evt) {
		if (evt.isOfType(ITuneStateChanged.class)) {
			toggleDataCollection(false);
		}
	}
}
