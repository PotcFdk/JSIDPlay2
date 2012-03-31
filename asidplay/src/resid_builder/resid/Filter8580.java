package resid_builder.resid;

public final class Filter8580 extends Filter {
	protected Filter8580() {
		super();
		setCurveAndDistortionDefaults();
	}

	/* Type4 params */
	private float type4_k, type4_b;

	private float type4_w0_cache;

	@Override
	protected final float clock(final int voice1, final int voice2, final int voice3, final int ext_in) {
		float Vi = 0.f, Vf = 0.f;

		// Route voices into or around filter.
		if (filt1) {
			Vi += voice1;
		} else {
			Vf += voice1;
		}
		if (filt2) {
			Vi += voice2;
		} else {
			Vf += voice2;
		}
		// NB! Voice 3 is not silenced by voice3off if it is routed through
		// the filter.
		if (filt3) {
			Vi += voice3;
		} else if (voice3off) {
			Vf += voice3;
		}
		if (filtE) {
			Vi += ext_in;
		} else {
			Vf += ext_in;
		}

		/* On the 8580, BP appears mixed in phase with the rest. */
		Vlp += Vbp * type4_w0_cache;
		Vbp += Vhp * type4_w0_cache;
		Vhp = -Vbp * _1_div_Q - Vlp - Vi;

		if (lp) {
			Vf += Vlp;
		}
		if (bp) {
			Vf += Vbp;
		}
		if (hp) {
			Vf += Vhp;
		}

		Vf *= vol;
		return Vf;
	}

	@Override
	public void setCurveAndDistortionDefaults() {
		setCurveProperties(6.55f, 20f, 0, 0);
	}

	@Override
	protected void setClockFrequency(final double clock) {
		super.setClockFrequency(clock);
		updatedCenterFrequency();
	}

	@Override
	public float[] getDistortionProperties() {
		return new float[] { 0f, 0f, resonanceFactor };
	}

	@Override
	public void setDistortionProperties(final float attenuation, final float Nonlinearity, final float resonanceFac) {
		resonanceFactor = resonanceFac;
		updatedCenterFrequency();
	}

	@Override
	public float[] getCurveProperties() {
		return new float[] { type4_k, type4_b, 0, 0 };
	}

	@Override
	public void setCurveProperties(final float k, final float b, final float ignored1, final float ignored2) {
		type4_k = k;
		type4_b = b;
		updatedCenterFrequency();
	}

	private float type4_w0() {
		final float freq = type4_k * fc + type4_b;
		return (float) (2 * Math.PI * freq / clockFrequency);
	}

	@Override
	protected void updatedCenterFrequency() {
		type4_w0_cache = type4_w0();
	}

	@Override
	protected void updatedResonance() {
		_1_div_Q = 1.f / (0.707f + resonanceFactor * res / 15.f);
	}
}
