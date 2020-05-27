package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_FADE_IN;

public class SetFadeIn implements NetSIDPkg {
	private final int fadeIn;

	public SetFadeIn(float fadeIn) {
		this.fadeIn = (int) (fadeIn * 1000);
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) SET_FADE_IN.ordinal(), 0, 0, 0, (byte) ((fadeIn >> 24) & 0xff),
				(byte) ((fadeIn >> 16) & 0xff), (byte) ((fadeIn >> 8) & 0xff), (byte) (fadeIn & 0xff) };
	}
}
