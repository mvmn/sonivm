package x.mvmn.sonivm.ui.retro;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;

public class ExtRasterUISlider extends RasterUISlider {

	protected final Function<Integer, Integer> sliderPositionToBackgroundIndex;
	protected final BufferedImage[] sliderBackgrounds;
	protected int handleXOffset = 0;
	protected int handleYOffset = 0;

	public ExtRasterUISlider(RasterGraphicsWindow parent, BufferedImage[] sliderBackgrounds, BufferedImage sliderButtonReleased,
			BufferedImage sliderButtonPressed, int x, int y, int range, int initialPosition, boolean vertical,
			Function<Integer, Integer> sliderPositionToBackgroundIndex) {
		super(parent, sliderBackgrounds[0], sliderButtonReleased, sliderButtonPressed, x, y, range, initialPosition, vertical);
		this.sliderPositionToBackgroundIndex = sliderPositionToBackgroundIndex;
		this.sliderBackgrounds = sliderBackgrounds;
		repaint();
	}

	public ExtRasterUISlider(RasterGraphicsWindow parent, BufferedImage[] sliderBackgrounds, BufferedImage sliderButtonReleased,
			BufferedImage sliderButtonPressed, int x, int y, int range, int initialPosition, boolean vertical,
			Function<Integer, Integer> sliderPositionToBackgroundIndex, int handleXOffset, int handleYOffset) {
		super(parent, sliderBackgrounds[0], sliderButtonReleased, sliderButtonPressed, x, y, range, initialPosition, vertical);
		this.sliderPositionToBackgroundIndex = sliderPositionToBackgroundIndex;
		this.sliderBackgrounds = sliderBackgrounds;
		this.handleXOffset = handleXOffset;
		this.handleYOffset = handleYOffset;
		repaint();
	}

	@Override
	public void repaint() {
		if (sliderPositionToBackgroundIndex == null) {
			return;
		}
		BufferedImage argbBackgroundImage = super.image;
		Graphics2D g2d = argbBackgroundImage.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		int backgroundToUse = sliderPositionToBackgroundIndex.apply(sliderPosition);
		if (backgroundToUse < 0) {
			backgroundToUse = 0;
		} else if (backgroundToUse >= sliderBackgrounds.length) {
			backgroundToUse = sliderBackgrounds.length - 1;
		}

		g2d.drawImage(sliderBackgrounds[backgroundToUse], 0, 0, null);
		int x = 0;
		int y = 0;
		if (vertical) {
			y += this.sliderPosition;
		} else {
			x += this.sliderPosition;
		}
		BufferedImage img = pressed ? sliderButtonPressed : sliderButtonReleased;
		g2d.drawImage(img, x + handleXOffset, y + handleYOffset, null);
		parent.repaintChildComponent(this);
	}
}
