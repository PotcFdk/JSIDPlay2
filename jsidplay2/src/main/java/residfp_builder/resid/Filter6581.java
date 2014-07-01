package residfp_builder.resid;

public final class Filter6581 extends Filter {

	protected Filter6581() {
		super();
		setCurveAndDistortionDefaults();
	}

	/* Distortion params */
	private float attenuation, nonlinearity;

	/* Type3 params */
	private float baseresistance, offset, steepness, minimumfetresistance, voiceNonlinearity;

	private static final double SIDCAPS_6581 = 470e-12;
	private static final float OSC_TO_FC = 1/512f;

	/* 1024 because real chip has disconnected line #0.
	 * 128 seems to suffice for the size of approximation:
	 * the bound is exceeded only during most extreme distortion. */
	private final float[][] type3_w0s = new float[1024][256];
	private float[] type3_w0;

	/* succeeding values differ by about 1 % at this resolution,
	 * so that feels suitable level of precision. Previously I worked
	 * with fastexp() (linear approximation of exp()) that had up to
	 * 3 % error.
	 */
	private static final float TYPE3_W0S_RESOLUTION = 1/5e3f;

	private float type3_w0(final float dist) {
		if (dist < 0) {
			return type3_w0[0];
		}
		final int index = (int) (dist * TYPE3_W0S_RESOLUTION);
		return type3_w0[index < 256 ? index : 255];
	}

	private float waveshaper1(float value) {
		if (value > nonlinearity) {
			value -= (value - nonlinearity) * 0.5f;
		}
		return value;
	}

	public float estimateCurrentDistortion() {
		return type3_w0(Vhp) + type3_w0(Vbp) - 2f * type3_w0(0);
	}

	@Override
	protected final float clock(final float voice1, final float voice2, final float voice3, final float ext_in) {
		float Vi = 0, Vf = 0;

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

		Vlp -= Vbp * type3_w0(Vbp);
		Vbp -= Vhp * type3_w0(Vhp);
		Vhp = (Vbp * _1_div_Q - Vlp - Vi) * attenuation;

		if (lp) {
			Vf += Vlp;
		}
		if (bp) {
			Vf += Vbp;
		}
		if (hp) {
			Vf += Vhp;
		}

		/* saturate. This is likely the output inverter saturation. */
		return waveshaper1(Vf * vol);
	}

	public void setNonLinearity(final float nl) {
		voiceNonlinearity = nl;
		recalculate();
		updatedCenterFrequency();
	}

	@Override
	public void setCurveAndDistortionDefaults() {
		setDistortionProperties(0.64f, 3.3e6f, 1.0f);
		setCurveProperties(1147036.5f, 2.742288e8f, 1.0066634f, 16125.155f);
		setNonLinearity(0.96131605f);
	}

	@Override
	protected void setClockFrequency(final double clock) {
		super.setClockFrequency(clock);
		recalculate();
		updatedCenterFrequency();
	}

	@Override
	public float[] getDistortionProperties() {
		return new float[] { attenuation, nonlinearity, resonanceFactor };
	}

	@Override
	public void setDistortionProperties(final float a, final float nl, final float rf) {
		attenuation = a;
		nonlinearity = nl;
		resonanceFactor = rf;
		updatedResonance();
	}

	@Override
	public float[] getCurveProperties() {
		return new float[] { baseresistance, offset, steepness, minimumfetresistance };
	}

	@Override
	public void setCurveProperties(final float br, final float o, final float s, final float mfr) {
		baseresistance = br;
		offset = o;
		steepness = s;
		minimumfetresistance = mfr;
		recalculate();
		updatedCenterFrequency();
	}

	@Override
	protected void updatedCenterFrequency() {
		type3_w0 = type3_w0s[fc >> 1];
	}

	private void recalculate() {
		final float[] fcBase = new float[1024];
		for (int j = 0; j < fcBase.length; j ++) {
			final float type3_fc_kink = SID.kinkedDac(j << 1, voiceNonlinearity, 11);
			fcBase[j] = offset / (float) Math.pow(steepness, type3_fc_kink);
		}

		final float[] distBase = new float[type3_w0s[0].length];
		for (int i = 0; i < distBase.length; i ++) {
			final float dist = i > 0 ? (i + 0.5f) / TYPE3_W0S_RESOLUTION : 0;
			distBase[i] = 1f / (float) Math.pow(steepness, dist * OSC_TO_FC);
		}

		final float _1_div_caps_freq = (float) (1 / (SIDCAPS_6581 * clockFrequency));

		/* Try to avoid overflow when filter curve is being changed.
		 * This isn't really right, the play thread should be suspended first...
		 */
		Vlp = Vhp = Vbp = 0;
		for (int j = 0; j < fcBase.length; j ++) {
			final float fcBaseValue = fcBase[j];
			for (int i = 0; i < distBase.length; i ++) {
				final float fetresistance = fcBaseValue * distBase[i];
				final float dynamic_resistance = minimumfetresistance + fetresistance;

				/* 2 parallel resistors */
				final float _1_div_resistance = (baseresistance + dynamic_resistance) / (baseresistance * dynamic_resistance);
				type3_w0s[j][i] = _1_div_caps_freq * _1_div_resistance;
			}
		}
		
	}

	@Override
	protected void updatedResonance() {
		/* XXX: resonance tuned by ear, based on a few observations:
		 * 
		 * - there's a small notch even in allpass mode
		 * - size of resonance hump is about 8 dB
		 */
		_1_div_Q = 1.f / (0.5f + resonanceFactor * res / 18f);
	}
}
