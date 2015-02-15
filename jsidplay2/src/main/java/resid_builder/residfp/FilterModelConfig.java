package resid_builder.residfp;

import sidplay.ini.intf.IFilterSection;

public final class FilterModelConfig {
	/* these constants seem fixed for all 6581s... */
	private static final double sid_caps = 470e-12;
	private static final float dac_kinkiness = 0.96f;

	public static double estimateFrequency(final IFilterSection filter,
			final int fc) {
		if (filter.getK() == 0f) {
			final double ik = SID.kinkedDac(fc, dac_kinkiness, 11);
			final double dynamic = filter.getMinimumfetresistance()
					+ filter.getOffset() / Math.pow(filter.getSteepness(), ik);
			final double R = filter.getBaseresistance() * dynamic
					/ (filter.getBaseresistance() + dynamic);
			return 1 / (2 * Math.PI * sid_caps * R);
		} else {
			return filter.getK() * fc + filter.getB();
		}
	}

}