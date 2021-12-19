package x.mvmn.sonivm.ui.retro;

import java.awt.image.BufferedImage;

public class RasterUIButton extends RasterUIComponent {

	public RasterUIButton(RasterGraphicsWindow parent, BufferedImage imageReleased, BufferedImage imagePressed, int x, int y) {
		super(parent, new BufferedImage(imageReleased.getWidth(), imageReleased.getHeight(), BufferedImage.TYPE_INT_ARGB), x, y);
	}
}
