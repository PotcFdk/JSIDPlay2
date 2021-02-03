package ui.oscilloscope;

public class GaugeImage {

	private int[] pixels;
	private String text;

	public GaugeImage(int[] pixels, String text) {
		this.pixels = pixels;
		this.text = text;
	}

	public int[] getPixels() {
		return pixels;
	}

	public String getText() {
		return text;
	}
}
