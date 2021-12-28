package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.image.BufferedImage;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;

public class RasterUIButton extends RasterUIComponent {

	protected final BufferedImage imageReleased;
	protected final BufferedImage imagePressed;

	public RasterUIButton(RasterGraphicsWindow parent, BufferedImage imageReleased, BufferedImage imagePressed, int x, int y) {
		super(parent, new BufferedImage(imageReleased.getWidth(), imageReleased.getHeight(), BufferedImage.TYPE_INT_ARGB), x, y);
		this.imageReleased = imageReleased;
		this.imagePressed = imagePressed;

		repaint();
	}

	@Override
	public void repaint() {
		ImageUtil.drawOnto(super.image, pressed && mouseInRange ? imagePressed : imageReleased, 0, 0);
		super.repaint();
	}
}
