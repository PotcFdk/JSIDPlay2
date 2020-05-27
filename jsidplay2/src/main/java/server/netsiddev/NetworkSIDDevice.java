package server.netsiddev;

import javax.swing.SwingUtilities;

import builder.resid.residfp.Filter6581;
import builder.resid.residfp.Filter8580;
import libsidplay.common.ChipModel;
import libsidplay.common.SIDChip;
import libsidplay.config.IFilterSection;
import server.netsiddev.ini.JSIDDeviceConfig;

public class NetworkSIDDevice {
	private static JSIDDeviceConfig config;

	/**
	 * Gets the number of known configurations.
	 * 
	 * @return The number of known SID configurations.
	 */
	public static byte getSidCount() {
		String[] sid = config.getFilterList();
		return (byte) sid.length;
	}

	/**
	 * Return the name of the requested SID.
	 * 
	 * @param sidNum
	 *            The SID to get the name of.
	 * @return SID name string
	 */
	protected static String getSidName(int sidNum) {
		String[] sid = config.getFilterList();
		return sid[sidNum];
	}

	/**
	 * Construct the SID object suite.
	 * 
	 * @param sidNumber
	 */
	protected static SIDChip getSidConfig(int sidNumber) {
		IFilterSection iniFilter = config.getFilter(config.getFilterList()[sidNumber]);

		SIDChip sid = null;
		if (iniFilter.isReSIDFilter6581()) {
			sid = new builder.resid.resid.SID();
			((builder.resid.resid.SID) sid).setChipModel(ChipModel.MOS6581);
			((builder.resid.resid.SID) sid).getFilter6581().setFilterCurve(iniFilter.getFilter6581CurvePosition());
		} else if (iniFilter.isReSIDFilter8580()) {
			sid = new builder.resid.resid.SID();
			((builder.resid.resid.SID) sid).setChipModel(ChipModel.MOS8580);
			((builder.resid.resid.SID) sid).getFilter8580().setFilterCurve(iniFilter.getFilter8580CurvePosition());
		} else if (iniFilter.isReSIDfpFilter6581()) {
			sid = new builder.resid.residfp.SID();
			((builder.resid.residfp.SID) sid).setChipModel(ChipModel.MOS6581);
			Filter6581 filter6581 = ((builder.resid.residfp.SID) sid).getFilter6581();
			filter6581.setCurveProperties(iniFilter.getBaseresistance(), iniFilter.getOffset(),
					iniFilter.getSteepness(), iniFilter.getMinimumfetresistance());
			filter6581.setDistortionProperties(iniFilter.getAttenuation(), iniFilter.getNonlinearity(),
					iniFilter.getResonanceFactor());
			((builder.resid.residfp.SID) sid).set6581VoiceNonlinearity(iniFilter.getVoiceNonlinearity());
			filter6581.setNonLinearity(iniFilter.getVoiceNonlinearity());
		} else if (iniFilter.isReSIDfpFilter8580()) {
			sid = new builder.resid.residfp.SID();
			((builder.resid.residfp.SID) sid).setChipModel(ChipModel.MOS8580);
			Filter8580 filter8580 = ((builder.resid.residfp.SID) sid).getFilter8580();
			filter8580.setCurveProperties(iniFilter.getK(), iniFilter.getB(), 0, 0);
			filter8580.setDistortionProperties(0, 0, iniFilter.getResonanceFactor());
		}
		return sid;
	}

	public void start(boolean createIniFileIfNotExists) {
		config = new JSIDDeviceConfig(createIniFileIfNotExists);
		new Thread(() -> {
			try {
				ClientContext.listenForClients(config);
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> printErrorAndExit(e));
			}
		}).start();
	}

	protected void printErrorAndExit(Exception e) {
		e.printStackTrace();
		System.exit(-1);
	}

	public static void main(String[] args) {
		new NetworkSIDDevice().start(true);
	}

}
