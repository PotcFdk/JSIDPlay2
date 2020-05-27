package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_FADE_OUT;

public class SetFadeOut implements NetSIDPkg {
	private final int fadeOut;

	public SetFadeOut(float fadeOut) {
		this.fadeOut = (int) (fadeOut * 1000);
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) SET_FADE_OUT.ordinal(), 0, 0, 0, (byte) ((fadeOut >> 24) & 0xff),
				(byte) ((fadeOut >> 16) & 0xff), (byte) ((fadeOut >> 8) & 0xff), (byte) (fadeOut & 0xff) };
	}
}
