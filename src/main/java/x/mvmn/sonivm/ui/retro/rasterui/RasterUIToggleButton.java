package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.image.BufferedImage;

import lombok.Getter;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;

public class RasterUIToggleButton extends RasterUIButton {

	protected final BufferedImage imageOffReleased;
	protected final BufferedImage imageOffPressed;
	@Getter
	protected volatile boolean buttonOn;

	public RasterUIToggleButton(RasterGraphicsWindow parent, BufferedImage imageOnReleased, BufferedImage imageOnPressed,
			BufferedImage imageOffReleased, BufferedImage imageOffPressed, int x, int y) {
		super(parent, imageOnReleased, imageOnPressed, x, y);
		this.imageOffReleased = imageOffReleased;
		this.imageOffPressed = imageOffPressed;
		prepareImage();
	}

	@Override
	public void repaint() {
		prepareImage();
		parent.repaintChildComponent(this);
	}

	protected void prepareImage() {
		BufferedImage image;
		if (pressed && mouseInRange) {
			image = buttonOn ? imagePressed : imageOffPressed;
		} else {
			image = buttonOn ? imageReleased : imageOffReleased;
		}
		ImageUtil.drawOnto(super.image, image, 0, 0);
	}

	@Override
	protected void onPressBeforeRepaint() {
		buttonOn = !buttonOn;
		super.onPressBeforeRepaint();
	}

	public void setButtonOn(boolean buttonOn) {
		this.buttonOn = buttonOn;
		repaint();
	}
}
